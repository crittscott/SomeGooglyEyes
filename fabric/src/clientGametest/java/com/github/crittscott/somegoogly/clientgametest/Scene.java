package com.github.crittscott.somegoogly.clientgametest;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Stages client-test scenarios and records their screenshots. A test class opens one world through
 * {@link #run} and runs its scenarios in order; each {@link #scenario} gets a fresh platform far
 * from the last, and everything a scenario changes (server settings, datapacks, game mode,
 * inventory, client view options) is reset after it.
 *
 * <p>Scenarios act as a player would wherever the plan checks what a player does: items are used
 * with the use and attack keys while aiming, and {@code /sg} commands go through the client's
 * command path. Setup that a player would do by hand (summoning, placing blocks, game rules) runs
 * as server commands.
 *
 * <p>A scenario that throws is recorded in the manifest as unfinished and the class moves on to the
 * next one, so one broken scenario costs only its own screenshots.
 */
final class Scene {
    static final String BLUE = "[0.1f,0.2f,1f]";
    static final String GOOGLY = "somegoogly:googly_eye";
    static final String GREEN = "[0.1f,0.8f,0.1f]";
    static final String MAIN = "";
    static final String OPTOMETRIST =
            "minecraft:shears[enchantments={\"somegoogly:optometrist\":1}]";
    static final String RED = "[1f,0f,0f]";
    static final String SLIMY = "somegoogly:slimy_eye";

    /**
     * The y a player or mob stands at on a scenario platform; the platform's top blocks are one
     * below.
     */
    static final int GROUND = -60;

    /** The crafting menu's first hotbar slot; slots 1 to 9 are the grid and 0 the result. */
    static final int CRAFTING_HOTBAR_START = 37;

    /** The player inventory menu's first hotbar slot; 9 to 35 are the main inventory. */
    static final int INVENTORY_HOTBAR_START = 36;

    /** Ticks waited after chunks finish drawing, before a shot or after a camera move. */
    private static final int SETTLE_TICKS = 20;

    /**
     * Field of view for shots: narrower than the default 70 so eyes on a subject a few blocks away
     * read clearly.
     */
    private static final int CLOSE_FOV = 50;

    /** Vanilla's default field of view, for scenes that frame a wide row of subjects. */
    private static final int WIDE_FOV = 70;

    /** Window size for shots; more pixels per eye than the test client's default window. */
    private static final int WINDOW_HEIGHT = 1080;

    private static final int WINDOW_WIDTH = 1920;

    /** Ticks for a client rotation change to reach the server. */
    private static final int AIM_TICKS = 3;

    /** How long an application may take before the scenario is failed for missing eyes. */
    private static final int EYES_TIMEOUT_TICKS = 40;

    private static final int AREA_HALF_WIDTH = 12;
    private static final int AREA_SPACING = 96;
    private static final String FIXTURES_PROPERTY = "somegoogly.clientgametest.fixtures";
    private static final String GROUP_PROPERTY = "somegoogly.clientgametest.group";
    private static final Logger LOGGER = LoggerFactory.getLogger("somegoogly-clientgametest");

    final ClientGameTestContext context;
    final TestInput input;
    private int areaIndex;
    private TestClientWorldContext clientWorld;
    private TestServerConnection connection;
    private TestDedicatedServerContext dedicated;
    private final Set<String> enabledPacks = new LinkedHashSet<>();
    private final List<String> failures = new ArrayList<>();
    private String originalConfig;
    private int ox;
    private int oz;
    private String scenarioId = "";
    private String scenarioTitle = "";
    private TestServerContext server;
    private TestSingleplayerContext singleplayer;
    private int tagCounter;

    private Scene(ClientGameTestContext context) {
        this.context = context;
        this.input = context.getInput();
    }

    /** The launch group this client was started for; empty for the main run. */
    static String launchGroup() {
        return System.getProperty(GROUP_PROPERTY, MAIN);
    }

    /** Runs {@code body} in a fresh singleplayer world when this launch is for {@code group}. */
    static void run(ClientGameTestContext context, String group, Consumer<Scene> body) {
        if (!launchGroup().equals(group)) {
            return;
        }
        Scene scene = new Scene(context);
        scene.openNewWorld(true);
        try {
            body.accept(scene);
        } finally {
            scene.closeWorld();
            scene.logFailures();
        }
    }

    /**
     * Runs {@code body} on a dedicated server this client is connected to, in the main launch only.
     */
    static void runDedicated(ClientGameTestContext context, Consumer<Scene> body) {
        if (!launchGroup().equals(MAIN)) {
            return;
        }
        Scene scene = new Scene(context);
        scene.dedicated = context.worldBuilder().createServer();
        scene.server = scene.dedicated;
        scene.connect();
        try {
            body.accept(scene);
        } finally {
            scene.connection.close();
            scene.dedicated.close();
            scene.logFailures();
        }
    }

    /**
     * Runs {@code /sg admin <args>} as the player, aimed at the tagged entity from where the player
     * stands.
     */
    void admin(String tag, String args) {
        // Looking straight down from just above the mob: nothing else, such as the ground around a
        // pit or a neighbor in a row, can be the first thing in the crosshair. A mob with AI is
        // held still for the command, so it cannot walk out from under the crosshair.
        boolean[] wasFree = new boolean[1];
        AABB box =
                serverValue(sv ->
                        tagged(sv, tag)
                                .map(e -> {
                                    if (e instanceof Mob mob && !mob.isNoAi()) {
                                        wasFree[0] = true;
                                        mob.setNoAi(true);
                                    }
                                    return e.getBoundingBox();
                                })
                                .orElseThrow(() ->
                                        new IllegalStateException("No entity tagged " + tag)));
        server(
                "tp @p %s %s %s",
                fmt(box.getCenter().x), fmt(box.maxY + 1.0), fmt(box.getCenter().z));
        waitTicks(2);
        fly();
        turn(0, 90);
        player("sg admin " + args);
        if (wasFree[0]) {
            serverValue(sv -> {
                tagged(sv, tag).ifPresent(e -> ((Mob) e).setNoAi(false));
                return true;
            });
        }
    }

    /** Sets ambient expressions to {@code pool} at a fixed interval, in one reload. */
    void ambient(int intervalTicks, String... pool) {
        config(
                "ambientBehaviors",
                "true",
                "ambientMinTicks",
                Integer.toString(intervalTicks),
                "ambientMaxTicks",
                Integer.toString(intervalTicks),
                "ambientBehaviorPool",
                tomlList(pool));
    }

    /** Presses the attack key once. */
    void attack() {
        input.pressKey(options().keyAttack);
        waitTicks(2);
    }

    /**
     * Takes {@code count} shots {@code spacing} ticks apart, for expressions whose start within an
     * interval is not fixed; the expectation describes the group, and each shot says where it falls
     * in it.
     */
    void burst(int count, int spacing, String expect) {
        settle();
        for (int i = 1; i <= count; i++) {
            snap("Shot " + i + " of " + count + ", " + spacing + " ticks apart. " + expect);
            if (i < count) {
                waitTicks(spacing);
            }
        }
    }

    /**
     * Summons a boat of the given entity type at an offset, resting on the platform or floating on
     * water there (the water surface is level with the platform top); returns its tag.
     */
    String boat(String type, double dx, double dz, float yaw) {
        String tag = newTag();
        server(
                "summon %s %s %s %s {Tags:[\"%s\"],Rotation:[%sf,0f]}",
                type, fmt(x(dx)), fmt(GROUND), fmt(z(dz)), tag, fmt(yaw));
        waitTicks(2);
        return tag;
    }

    /** Clicks a slot of the open container menu the way a mouse click on that slot would. */
    void click(int slot, int button, ClickType type) {
        context.runOnClient(mc ->
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId, slot, button, type, mc.player));
        waitTicks(2);
    }

    /**
     * Crafts in a crafting table at the platform origin: gives {@code ingredients} into an emptied
     * hotbar, opens the table, and moves each into the grid. The screen stays open showing the
     * result; {@link #takeCraftResult} moves the result into hotbar slot 0.
     */
    void craft(String... ingredients) {
        clearInventory();
        for (String ingredient : ingredients) {
            give(ingredient);
        }
        craftFromHotbar(ingredients.length);
    }

    /** Like {@link #craft}, with whatever is already in the first {@code count} hotbar slots. */
    void craftFromHotbar(int count) {
        setBlock(0, 0, 2, "minecraft:crafting_table");
        cameraAt(0, 0, 0, 0, 0.5, 2);
        use();
        context.waitForScreen(CraftingScreen.class);
        for (int i = 0; i < count; i++) {
            click(CRAFTING_HOTBAR_START + i, 0, ClickType.PICKUP);
            click(1 + i, 0, ClickType.PICKUP);
        }
        waitTicks(3);
    }

    /**
     * Moves the shown crafting result into hotbar slot 0, which the first ingredient left empty,
     * and closes.
     */
    void takeCraftResult() {
        click(0, 0, ClickType.PICKUP);
        click(CRAFTING_HOTBAR_START, 0, ClickType.PICKUP);
        closeScreen();
        hotbar(0);
    }

    /** Puts an item into an equipment or inventory slot by {@code /item} slot name. */
    void slot(String slot, String item) {
        server("item replace entity @p %s with %s", slot, item);
        waitTicks(2);
    }

    /**
     * Uses whatever the player holds on the tagged entity from a nearby spot, without changing the
     * inventory.
     */
    void useHeldOn(String tag, double dx, double dy, double dz) {
        cameraOn(dx, dy, dz, tag);
        // A survival player may have fallen or sunk while the camera settled; aim again from where
        // it is now.
        aimAt(tag);
        use();
    }

    /** Sneak-uses {@code item} at the sky, the way a player applies or removes their own eyes. */
    void useOnSelf(String item) {
        clearInventory();
        give(item);
        hotbar(0);
        turn(context.computeOnClient(mc -> mc.player.getYRot()), -90);
        hold(options().keyShift);
        waitTicks(2);
        use();
        release(options().keyShift);
    }

    /**
     * Writes server settings without {@code /reload}, so a scenario can show the file and the
     * effective settings disagreeing until the next reload or world start.
     */
    void writeConfig(String... keyValues) {
        Path path = configPath();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (originalConfig == null) {
                originalConfig = text;
            }
            for (int i = 0; i < keyValues.length; i += 2) {
                text = replaceKey(text, keyValues[i], keyValues[i + 1]);
            }
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes raw text as the server config without {@code /reload}; the original is restored
     * afterward.
     */
    void writeConfigText(String text) {
        Path path = configPath();
        try {
            if (originalConfig == null) {
                originalConfig = Files.readString(path, StandardCharsets.UTF_8);
            }
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The server config text as it is on disk now. */
    String readConfig() {
        try {
            return Files.readString(configPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Moves the mouse over a slot of the open container screen, so its tooltip shows. */
    void hoverSlot(int slot, int imageWidth, int imageHeight) {
        double[] position =
                context.computeOnClient(mc -> {
                    Slot target = mc.player.containerMenu.getSlot(slot);
                    int left = (mc.getWindow().getGuiScaledWidth() - imageWidth) / 2;
                    int top = (mc.getWindow().getGuiScaledHeight() - imageHeight) / 2;
                    double scale = mc.getWindow().getGuiScale();
                    return new double[] {
                        (left + target.x + 8) * scale, (top + target.y + 8) * scale
                    };
                });
        input.setCursorPos(position[0], position[1]);
        waitTicks(2);
    }

    /** Places the camera at an offset from the platform origin, at the given rotation. */
    void camera(double dx, double dy, double dz, float yaw, float pitch) {
        moveTo(dx, dy, dz);
        turn(yaw, pitch);
        settle();
    }

    /** Places the camera at an offset, looking at a point given as another offset. */
    void cameraAt(double dx, double dy, double dz, double tx, double ty, double tz) {
        moveTo(dx, dy, dz);
        aimAt(x(tx), GROUND + ty, z(tz));
        settle();
    }

    /** Places the camera at an offset, looking at the middle of the tagged entity. */
    void cameraOn(double dx, double dy, double dz, String tag) {
        moveTo(dx, dy, dz);
        aimAt(tag);
        settle();
    }

    /**
     * Turns the player's view toward a world point, the way moving the mouse does: the client's
     * rotation changes and reaches the server in the next movement packet. A server-side {@code tp
     * ... facing} is not used, because the client's own rotation can overwrite it before a
     * following command reads where the player looks.
     */
    void aimAt(double wx, double wy, double wz) {
        context.runOnClient(mc -> {
            Vec3 eye = mc.player.getEyePosition();
            double ddx = wx - eye.x;
            double ddy = wy - eye.y;
            double ddz = wz - eye.z;
            float yaw = (float) Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0F;
            float pitch =
                    (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
            rotate(mc.player, yaw, pitch);
        });
        waitTicks(AIM_TICKS);
    }

    /** Turns the player's view toward the middle of the tagged entity. */
    void aimAt(String tag) {
        Vec3 centre =
                serverValue(sv ->
                        tagged(sv, tag)
                                .map(e -> e.getBoundingBox().getCenter())
                                .orElseThrow(() ->
                                        new IllegalStateException("No entity tagged " + tag)));
        aimAt(centre.x, centre.y, centre.z);
    }

    /** Sets the player's view rotation directly. */
    void turn(float yaw, float pitch) {
        context.runOnClient(mc -> rotate(mc.player, yaw, pitch));
        waitTicks(AIM_TICKS);
    }

    /**
     * Moves the player to an offset from the platform origin, keeping its rotation, and keeps it
     * flying.
     */
    void moveTo(double dx, double dy, double dz) {
        server("tp @p %s %s %s", fmt(x(dx)), fmt(GROUND + dy), fmt(z(dz)));
        waitTicks(2);
        fly();
    }

    private static void rotate(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
    }

    /**
     * Fails the scenario unless the tagged entity has eyes on the server, so a failed application
     * is reported.
     */
    void requireEyes(String tag) {
        if (!waitUntilTagged(tag, EyeState::hasEyes, EYES_TIMEOUT_TICKS)) {
            throw new IllegalStateException("Entity " + tag + " did not get eyes");
        }
        waitTicks(SETTLE_TICKS);
    }

    /** Clears the player's inventory. */
    void clearInventory() {
        server("clear @p");
    }

    /** Closes whatever screen is open. */
    void closeScreen() {
        context.setScreen(() -> null);
        waitTicks(2);
    }

    /**
     * Writes server settings into the world's {@code somegoogly-server.toml} as key/value pairs and
     * applies them with {@code /reload}. The file's text before the first change is restored when
     * the scenario ends.
     */
    void config(String... keyValues) {
        Path path = configPath();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (originalConfig == null) {
                originalConfig = text;
            }
            for (int i = 0; i < keyValues.length; i += 2) {
                text = replaceKey(text, keyValues[i], keyValues[i + 1]);
            }
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        reload();
    }

    /** The world's server config file. */
    Path configPath() {
        return server.computeOnServer(s -> s.getWorldPath(LevelResource.ROOT))
                .resolve("serverconfig")
                .resolve("somegoogly-server.toml");
    }

    /**
     * Builds a closed, unlit stone room around the platform origin and sets the time to midnight.
     */
    void darkRoom() {
        server(
                "fill %d %d %d %d %d %d minecraft:stone hollow",
                ox - 5, GROUND - 1, oz - 5, ox + 5, GROUND + 6, oz + 5);
        server("time set midnight");
    }

    /** Enables one of the {@link TestPacks}; packs are disabled again when the scenario ends. */
    void datapack(String pack) {
        server("datapack enable %s", TestPacks.commandId(pack));
        enabledPacks.add(pack);
        waitTicks(10);
    }

    /** Disables one of the {@link TestPacks}. */
    void datapackOff(String pack) {
        server("datapack disable %s", TestPacks.commandId(pack));
        enabledPacks.remove(pack);
        waitTicks(10);
    }

    /** Disconnects from the dedicated server and connects again. */
    void reconnect() {
        connection.close();
        connect();
    }

    /**
     * Fills a box between two block offsets from the platform origin, {@code dy} measured from
     * {@link #GROUND}.
     */
    void fill(int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, String block) {
        server(
                "fill %d %d %d %d %d %d %s",
                ox + dx1, GROUND + dy1, oz + dz1, ox + dx2, GROUND + dy2, oz + dz2, block);
    }

    /** Places an eyed head: the head block, then a Slimy Eye used on it by the player. */
    void eyedHead(String block, int dx, int dy, int dz) {
        eyedHead(block, dx, dy, dz, SLIMY);
    }

    /**
     * Places an eyed head with the given Slimy Eye item, so the head takes that item's appearance.
     */
    void eyedHead(String block, int dx, int dy, int dz, String slimyItem) {
        setBlock(dx, dy, dz, block);
        useOnBlock(dx, dy, dz, slimyItem);
        BlockPos pos = new BlockPos(ox + dx, GROUND + dy, oz + dz);
        if (!waitUntil(
                sv ->
                        sv.overworld().getBlockEntity(pos) instanceof SkullBlockEntity skull
                                && SkullEyeState.hasEyes(skull),
                EYES_TIMEOUT_TICKS)) {
            throw new IllegalStateException("Head at " + pos.toShortString() + " did not get eyes");
        }
        waitTicks(SETTLE_TICKS);
    }

    /**
     * Puts an eyed head item in the player's emptied inventory the way a player gets one: place the
     * head, apply a Slimy Eye, break it, and pick up the drop.
     */
    void eyedHeadItem(String headBlock) {
        eyedHeadItem(headBlock, SLIMY);
    }

    /** Like {@link #eyedHeadItem(String)}, with the given Slimy Eye item. */
    void eyedHeadItem(String headBlock, String slimyItem) {
        int dx = AREA_HALF_WIDTH - 2;
        eyedHead(headBlock, dx, 0, dx, slimyItem);
        clearInventory();
        server("setblock %d %d %d minecraft:air destroy", ox + dx, GROUND, oz + dx);
        server("tp @p %s %d %s", fmt(x(dx)), GROUND, fmt(z(dx)));
        waitTicks(10);
    }

    /**
     * Gives the tagged mob eyes with {@code /sg admin eyes true}, then any further admin commands.
     */
    void eyes(String tag, String... adminArgs) {
        admin(tag, "eyes true");
        requireEyes(tag);
        for (String args : adminArgs) {
            admin(tag, args);
        }
    }

    /** The folder holding owner-supplied fixtures, such as a world saved by an earlier release. */
    static Optional<Path> fixtures() {
        String property = System.getProperty(FIXTURES_PROPERTY);
        return property == null ? Optional.empty() : Optional.of(Path.of(property));
    }

    /**
     * Summons an item frame holding {@code itemSnbt} at an offset, with a stone block behind it so
     * it has something to hang on. {@code facing} is the direction the frame faces.
     */
    String frame(
            int dx,
            int dy,
            int dz,
            String facing,
            String itemSnbt,
            int rotation,
            boolean glow,
            boolean invisible) {
        int[] back =
                switch (facing) {
                    case "down" -> new int[] {0, 1, 0};
                    case "up" -> new int[] {0, -1, 0};
                    case "north" -> new int[] {0, 0, 1};
                    case "south" -> new int[] {0, 0, -1};
                    case "west" -> new int[] {1, 0, 0};
                    case "east" -> new int[] {-1, 0, 0};
                    default -> throw new IllegalArgumentException(facing);
                };
        int facingId =
                switch (facing) {
                    case "down" -> 0;
                    case "up" -> 1;
                    case "north" -> 2;
                    case "south" -> 3;
                    case "west" -> 4;
                    default -> 5;
                };
        setBlock(dx + back[0], dy + back[1], dz + back[2], "minecraft:stone");
        String tag = newTag();
        String item = itemSnbt.isEmpty() ? "" : "Item:" + itemSnbt + ",";
        // A frame summoned with an item is fixed so it cannot fall or be emptied by stray clicks;
        // an empty one is left loose so the player can put an item in it.
        int bx = ox + dx;
        int by = GROUND + dy;
        int bz = oz + dz;
        server(
                "summon minecraft:%s %d %d %d {%sblock_pos:[I;%d,%d,%d],Facing:%db,ItemRotation:%db,Invisible:%db,Fixed:%db,Tags:[\"%s\"]}",
                glow ? "glow_item_frame" : "item_frame",
                bx,
                by,
                bz,
                item,
                bx,
                by,
                bz,
                facingId,
                rotation,
                invisible ? 1 : 0,
                itemSnbt.isEmpty() ? 0 : 1,
                tag);
        return tag;
    }

    /** Sets the game mode; creative also resumes flying so the camera holds still. */
    void gamemode(String mode) {
        server("gamemode %s @p", mode);
        waitTicks(3);
        if (mode.equals("creative")) {
            fly();
        }
    }

    /** Gives the player an item, by {@code /give} item syntax. */
    void give(String item) {
        give(item, 1);
    }

    /** Gives the player {@code count} of an item, by {@code /give} item syntax. */
    void give(String item, int count) {
        server("give @p %s %d", item, count);
        waitTicks(2);
    }

    /** {@code /give} syntax for a Googly Eye with the given {@code eye_properties} SNBT. */
    static String googly(String properties) {
        return GOOGLY + "[somegoogly:eye_properties=" + properties + "]";
    }

    /** Entity-NBT item syntax for a Googly Eye with the given {@code eye_properties} SNBT. */
    static String googlyNbt(String properties) {
        return "{id:\""
                + GOOGLY
                + "\",count:1,components:{\"somegoogly:eye_properties\":"
                + properties
                + "}}";
    }

    /** Holds a key down until {@link #release}. */
    void hold(KeyMapping key) {
        input.holdKey(key);
    }

    /** Selects a hotbar slot, 0 to 8, with its number key. */
    void hotbar(int slot) {
        input.pressKey(options().keyHotbarSlots[slot]);
        waitTicks(1);
    }

    /** Shows or hides the HUD. */
    void hud(boolean shown) {
        context.runOnClient(mc -> mc.options.hideGui = !shown);
    }

    /** Summons a mob with its AI running at an offset; returns its tag. */
    String liveMob(String type, double dx, double dz, float yaw, String extraNbt) {
        return summon(type, dx, 0, dz, yaw, extraNbt);
    }

    /** Summons a still mob ({@code NoAI}) at an offset; returns its tag. */
    String mob(String type, double dx, double dz, float yaw) {
        return mob(type, dx, 0, dz, yaw, "");
    }

    /** Summons a still mob ({@code NoAI}) at an offset with extra NBT; returns its tag. */
    String mob(String type, double dx, double dy, double dz, float yaw, String extraNbt) {
        return summon(
                type, dx, dy, dz, yaw, extraNbt.isEmpty() ? "NoAI:1b" : "NoAI:1b," + extraNbt);
    }

    /** The current client options. */
    Options options() {
        return context.computeOnClient(mc -> mc.options);
    }

    /**
     * Runs a command as the player, through the client's command path, as typing it in chat would.
     */
    void player(String command) {
        context.runOnClient(mc -> mc.player.connection.sendCommand(command));
        waitTicks(3);
    }

    /** Presses a key once. */
    void press(KeyMapping key) {
        input.pressKey(key);
        waitTicks(2);
    }

    /** Releases a key held by {@link #hold}. */
    void release(KeyMapping key) {
        input.releaseKey(key);
        waitTicks(1);
    }

    /** Puts back the server config text from before this scenario's first change, and reloads. */
    void restoreConfig() {
        if (originalConfig == null) {
            return;
        }
        try {
            Files.writeString(configPath(), originalConfig, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        originalConfig = null;
        reload();
    }

    /** Runs {@code /reload} and waits for it to reach the client. */
    void reload() {
        server("reload");
        waitTicks(10);
    }

    /** Saves and quits the singleplayer world, then opens it again. */
    void reopen() {
        TestWorldSave save = singleplayer.getWorldSave();
        singleplayer.close();
        bindSingleplayer(save.open());
    }

    /**
     * Saves and quits the singleplayer world, replaces its save with a fixture's files, and opens
     * it.
     */
    void reopenFrom(Path fixture) {
        TestWorldSave save = singleplayer.getWorldSave();
        Path saveDirectory = save.getSaveDirectory();
        singleplayer.close();
        copyTree(fixture, saveDirectory);
        bindSingleplayer(save.open());
    }

    /** Runs a scenario on a fresh platform; a failure is recorded and the next scenario runs. */
    void scenario(String id, String title, Runnable body) {
        scenarioId = id;
        scenarioTitle = title;
        LOGGER.info("Client test {} {}: started", id, title);
        try {
            area();
            body.run();
            LOGGER.info("Client test {} {}: finished", id, title);
        } catch (RuntimeException | AssertionError e) {
            LOGGER.warn("Client test {} {}: did not finish", id, title, e);
            failures.add(id + ": " + e);
            ReviewRun.get().error(id, title, String.valueOf(e));
        } finally {
            resetScenario();
        }
    }

    /** The selector for a tagged entity. */
    static String sel(String tag) {
        return "@e[tag=" + tag + ",limit=1]";
    }

    /** Runs a command as the server, formatted with {@link Locale#ROOT}. */
    void server(String format, Object... args) {
        server.runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    /** Computes a value on the server thread. */
    <T> T serverValue(Function<MinecraftServer, T> function) {
        return server.computeOnServer(function::apply);
    }

    /** Sets a block at an offset from the platform origin. */
    void setBlock(int dx, int dy, int dz, String block) {
        server("setblock %d %d %d %s", ox + dx, GROUND + dy, oz + dz, block);
    }

    /**
     * Takes a screenshot for the current scenario, with what a correct one shows, once the world
     * around the camera has finished drawing.
     */
    void shot(String expect) {
        settle();
        snap(expect);
    }

    /**
     * Waits until every chunk in view has been built and drawn, then a few ticks for entity data to
     * arrive.
     */
    private void settle() {
        clientWorld.waitForChunksRender();
        waitTicks(SETTLE_TICKS);
    }

    /** Takes the screenshot now, with no settling, for shots whose timing is the point. */
    private void snap(String expect) {
        Path screenshot =
                context.takeScreenshot(scenarioId.replace('.', '-') + "_" + (++tagCounter));
        ReviewRun.get().shot(scenarioId, scenarioTitle, screenshot, expect);
    }

    /** The tagged entity on the server, if it exists. */
    static Optional<Entity> tagged(MinecraftServer server, String tag) {
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Entity> found =
                    level.getEntities(
                            EntityTypeTest.forClass(Entity.class),
                            entity -> entity.getTags().contains(tag));
            if (!found.isEmpty()) {
                return Optional.of(found.getFirst());
            }
        }
        return Optional.empty();
    }

    /** Uses the selected item on whatever the camera is aimed at. */
    void use() {
        input.pressKey(options().keyUse);
        waitTicks(3);
    }

    /** Gives {@code item} into an emptied inventory and uses it on the block at an offset. */
    void useOnBlock(int dx, int dy, int dz, String item) {
        clearInventory();
        give(item);
        hotbar(0);
        moveTo(dx, dy, dz + 2.5);
        aimAt(x(dx), GROUND + dy + 0.25, z(dz));
        use();
    }

    /**
     * Gives {@code item} into an emptied inventory and uses it on the tagged entity from a nearby
     * spot.
     */
    void useOnEntity(String tag, double dx, double dy, double dz, String item) {
        clearInventory();
        give(item);
        hotbar(0);
        useHeldOn(tag, dx, dy, dz);
        // Every boat a scenario applies a Slimy Eye to is meant to end up eyed; fail early if it
        // did not.
        boolean boat =
                serverValue(
                        sv -> tagged(sv, tag).map(e -> e instanceof AbstractBoat).orElse(false));
        if (boat && item.startsWith(SLIMY)) {
            requireEyes(tag);
        }
    }

    /** Sets a client view option: camera perspective. */
    void view(CameraType type) {
        context.runOnClient(mc -> mc.options.setCameraType(type));
        waitTicks(2);
    }

    /** Waits until a server-side condition holds, up to a limit; returns whether it did. */
    boolean waitUntil(Predicate<MinecraftServer> condition, int timeoutTicks) {
        for (int i = 0; i < timeoutTicks; i++) {
            if (server.computeOnServer(condition::test)) {
                return true;
            }
            context.waitTick();
        }
        return false;
    }

    /**
     * Waits until the tagged entity satisfies a server-side condition, up to a limit; returns
     * whether it did.
     */
    boolean waitUntilTagged(String tag, Predicate<Entity> condition, int timeoutTicks) {
        return waitUntil(s -> tagged(s, tag).filter(condition).isPresent(), timeoutTicks);
    }

    /** Waits a number of game ticks. */
    void waitTicks(int ticks) {
        context.waitTicks(ticks);
    }

    /** Replaces the platform between two offsets with one layer of water, for boats. */
    void water(int dx1, int dz1, int dx2, int dz2) {
        server(
                "fill %d %d %d %d %d %d minecraft:water",
                ox + dx1, GROUND - 1, oz + dz1, ox + dx2, GROUND - 1, oz + dz2);
    }

    /** World x of an offset from the platform origin's block center. */
    double x(double dx) {
        return ox + 0.5 + dx;
    }

    /** World z of an offset from the platform origin's block center. */
    double z(double dz) {
        return oz + 0.5 + dz;
    }

    /** Opens a new singleplayer world in place of the current one, prepared the standard way. */
    void newWorld(boolean allowCommands) {
        closeWorld();
        openNewWorld(allowCommands);
    }

    /** A new entity tag, unique within this test class. */
    String newTag() {
        return "sgt" + (++tagCounter);
    }

    /**
     * Widens the field of view to vanilla's default for the rest of the scenario, for scenes
     * framing a long row.
     */
    void wideView() {
        context.runOnClient(mc -> mc.options.fov().set(WIDE_FOV));
        waitTicks(2);
    }

    /** Sets the main arm. */
    void mainArm(HumanoidArm arm) {
        context.runOnClient(mc -> {
            mc.options.mainHand().set(arm);
            mc.options.broadcastOptions();
        });
        waitTicks(2);
    }

    /** Sets the GUI scale and lets the window lay out again. */
    void guiScale(int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
        waitTicks(2);
    }

    /**
     * Moves to a fresh platform; {@link #scenario} does this first, and a scenario that opens a new
     * world again.
     */
    void area() {
        areaIndex++;
        ox = areaIndex * AREA_SPACING;
        oz = 0;
        server("tp @p %d %d %d 0 30", ox, GROUND + 3, oz - 8);
        waitTicks(5);
        fly();
        clientWorld.waitForChunksRender();
        int w = AREA_HALF_WIDTH;
        server(
                "fill %d %d %d %d %d %d minecraft:stone",
                ox - w, GROUND - 4, oz - w, ox + w, GROUND - 2, oz + w);
        server(
                "fill %d %d %d %d %d %d minecraft:grass_block",
                ox - w, GROUND - 1, oz - w, ox + w, GROUND - 1, oz + w);
        server(
                "fill %d %d %d %d %d %d minecraft:air",
                ox - w, GROUND, oz - w, ox + w, GROUND + 20, oz + w);
        server("kill @e[type=!minecraft:player,x=%d,y=%d,z=%d,distance=..40]", ox, GROUND, oz);
        waitTicks(2);
    }

    private void bindSingleplayer(TestSingleplayerContext context) {
        singleplayer = context;
        server = context.getServer();
        clientWorld = context.getClientWorld();
        clientWorld.waitForChunksRender();
        prepareWorld();
    }

    private void closeWorld() {
        if (singleplayer != null) {
            singleplayer.close();
            singleplayer = null;
        }
    }

    private void connect() {
        connection = dedicated.connect();
        clientWorld = connection.getClientWorld();
        clientWorld.waitForChunksRender();
        server("op @a");
        waitTicks(5);
        prepareWorld();
    }

    private static void copyTree(Path from, Path to) {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path source : (Iterable<Path>) paths::iterator) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Keeps a creative player flying, so a teleport into the air holds the camera still instead of
     * falling. Right after a switch to creative the client may not have the new abilities yet, so
     * it waits briefly for them.
     */
    private void fly() {
        boolean creative =
                serverValue(sv ->
                        sv.getPlayerList().getPlayers().stream()
                                .anyMatch(p -> p.getAbilities().mayfly));
        if (!creative) {
            return;
        }
        for (int i = 0;
                i < 20 && !context.computeOnClient(mc -> mc.player.getAbilities().mayfly);
                i++) {
            context.waitTick();
        }
        context.runOnClient(mc -> {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(0, 0, 0);
            mc.player.onUpdateAbilities();
        });
        waitTicks(1);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private void logFailures() {
        if (failures.isEmpty()) {
            return;
        }
        LOGGER.warn("Client test scenarios that did not finish: {}", String.join("; ", failures));
    }

    private void openNewWorld(boolean allowCommands) {
        bindSingleplayer(
                context.worldBuilder()
                        .adjustSettings(creator -> creator.setAllowCommands(allowCommands))
                        .create());
    }

    private void prepareWorld() {
        // Natural rolls off, so a summoned mob has eyes only when a scenario gives them. This write
        // is the world's baseline, not a scenario change, so it is not restored. A file a scenario
        // deliberately broke is left as it is.
        Path path = configPath();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (Pattern.compile("(?m)^\\s*globalPercent\\s*=").matcher(text).find()) {
                Files.writeString(
                        path, replaceKey(text, "globalPercent", "0"), StandardCharsets.UTF_8);
                reload();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String rule :
                List.of(
                        "doDaylightCycle",
                        "doWeatherCycle",
                        "doMobSpawning",
                        "mobGriefing",
                        "doInsomnia",
                        "doPatrolSpawning",
                        "doTraderSpawning")) {
            server("gamerule %s false", rule);
        }
        server("time set noon");
        server("weather clear");
        // The test world starts in survival; scenarios assume creative unless they switch.
        server("gamemode creative @p");
        context.runOnClient(mc -> {
            mc.options.showSubtitles().set(true);
            // Shots are silent; subtitles still show which sounds played.
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(CLOSE_FOV);
        });
        input.resizeWindow(WINDOW_WIDTH, WINDOW_HEIGHT);
        fly();
    }

    private static String replaceKey(String text, String key, String value) {
        Pattern pattern =
                Pattern.compile(
                        "(?m)^(\\s*)" + Pattern.quote(key) + "\\s*=\\s*(\\[[^\\]]*\\]|[^\\r\\n]*)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("Server config has no key " + key);
        }
        return matcher.replaceFirst(
                Matcher.quoteReplacement(matcher.group(1) + key + " = " + value));
    }

    private void resetScenario() {
        context.setScreen(() -> null);
        for (KeyMapping key :
                List.of(
                        options().keyUse,
                        options().keyAttack,
                        options().keyShift,
                        options().keyUp,
                        options().keyDown,
                        options().keyLeft,
                        options().keyRight,
                        options().keyJump)) {
            input.releaseKey(key);
        }
        restoreConfig();
        for (String pack : List.copyOf(enabledPacks)) {
            datapackOff(pack);
        }
        server("gamemode creative @p");
        server("clear @p");
        server("effect clear @p");
        server("gamerule doMobLoot true");
        server("time set noon");
        context.runOnClient(mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(CLOSE_FOV);
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.options.broadcastOptions();
            mc.options.guiScale().set(0);
            mc.resizeDisplay();
        });
        waitTicks(3);
        fly();
    }

    private String summon(
            String type, double dx, double dy, double dz, float yaw, String extraNbt) {
        String tag = newTag();
        String extra = extraNbt.isEmpty() ? "" : "," + extraNbt;
        server(
                "summon %s %s %s %s {Tags:[\"%s\"],Rotation:[%sf,0f],PersistenceRequired:1b%s}",
                type, fmt(x(dx)), fmt(GROUND + dy), fmt(z(dz)), tag, fmt(yaw), extra);
        waitTicks(2);
        return tag;
    }

    /** A TOML string array, for list-valued server settings. */
    static String tomlList(String... values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            builder.append(i == 0 ? "" : ", ").append('"').append(values[i]).append('"');
        }
        return builder.append(']').toString();
    }
}
