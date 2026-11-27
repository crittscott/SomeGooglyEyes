package com.github.crittscott.somegoogly.clientgametest;

import com.github.crittscott.somegoogly.registry.ModContent;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/**
 * Client test plan section 8: eye items in frames, the GUI, hands, on the ground, and in several
 * places at once.
 */
public final class DisplayClientTests implements FabricClientGameTest {
    private static final String BLUE = Scene.googly("{irisColor:" + Scene.BLUE + "}");
    private static final String RED = Scene.googly("{irisColor:" + Scene.RED + "}");
    private static final String RED_NBT = Scene.googlyNbt("{irisColor:" + Scene.RED + "}");

    /** Survival inventory screen size, for hovering a slot. */
    private static final int INVENTORY_HEIGHT = 166;

    private static final int INVENTORY_WIDTH = 176;

    /** Frames an eyed head item: a fresh head item, a loose frame, and the player putting it in. */
    private static String framedHead(
            Scene s, String headBlock, int dx, int dy, int dz, String facing, int rotation) {
        s.eyedHeadItem(headBlock);
        String frame = s.frame(dx, dy, dz, facing, "", 0, false, false);
        s.hotbar(0);
        double standDz = facing.equals("south") ? dz + 2 : dz;
        double standDy = facing.equals("up") ? dy + 2 : facing.equals("down") ? dy - 2 : dy;
        s.useHeldOn(frame, dx, standDy, standDz);
        s.server("data merge entity %s {ItemRotation:%db}", Scene.sel(frame), rotation);
        return frame;
    }

    /**
     * In-game: eyed head items of every eligible type in frames, and zombie heads at several
     * orientations.
     */
    private static void framedHeads(Scene s) {
        String[] heads = {
            "skeleton_skull",
            "wither_skeleton_skull",
            "zombie_head",
            "creeper_head",
            "piglin_head",
            "player_head"
        };
        for (int i = 0; i < heads.length; i++) {
            framedHead(s, "minecraft:" + heads[i] + "[rotation=0]", -5 + i * 2, 2, -3, "south", 0);
        }
        for (int i = 0; i < 4; i++) {
            framedHead(s, "minecraft:zombie_head[rotation=0]", -3 + i * 2, 4, -3, "south", i * 2);
        }
        framedHead(s, "minecraft:zombie_head[rotation=0]", -2, 0, 2, "up", 0);
        framedHead(s, "minecraft:zombie_head[rotation=0]", 2, 0, 2, "up", 4);
        s.ambient(20, "blink");
        s.cameraAt(0, 3, 4, 0, 2.5, -3);
        s.burst(
                3,
                6,
                "Middle row: six head items (skeleton, wither skeleton, zombie, creeper, piglin, player) in frames, normal size, eyes in their configured places and settled. Top row: zombie heads at four rotations. In at least one shot a head blinks.");
        s.ambient(20, "cross_eye");
        s.burst(
                3,
                6,
                "Cross-eye-only pool: in at least one shot a framed head's paired eyes cross.");
        s.cameraAt(0, 4, 5, 0, 0, 2);
        s.shot(
                "Two zombie heads in floor frames at different rotations: eyes in place, pupils settled.");
    }

    /**
     * In-game: noon and midnight views of white, red, and glowing eyes in regular and glow frames.
     */
    private static void frameAppearance(Scene s) {
        s.ambient(20, "blink");
        String[] items = {Scene.googlyNbt("{}"), RED_NBT, Scene.googlyNbt("{glow:1b}")};
        for (int i = 0; i < items.length; i++) {
            s.frame(-2 + i * 2, 1, -3, "south", items[i], 0, false, false);
            s.frame(-2 + i * 2, 3, -3, "south", items[i], 0, true, false);
        }
        s.cameraAt(0, 2, 2, 0, 2, -3);
        s.burst(
                3,
                6,
                "Noon. Bottom row in regular frames, top row in glow frames: white, red, and glowing eyes, each much larger than an item icon, with normal thickness and colors. In at least one shot an eye blinks; a glowing eye keeps glowing while blinking.");
        s.darkRoom();
        s.burst(
                3,
                6,
                "The same frames inside a dark room at midnight: the glowing eyes stay bright (also while blinking); the others are lit only as frames are.");
    }

    /** In-game: invisible frames on a wall, the floor, and the ceiling. */
    private static void invisibleFrames(Scene s) {
        s.ambient(20, "blink");
        s.frame(0, 1, -3, "south", RED_NBT, 0, false, true);
        s.frame(-2, 0, 0, "up", RED_NBT, 0, false, true);
        s.frame(2, 3, 0, "down", RED_NBT, 0, false, true);
        s.cameraAt(0, 1.5, 4, 0, 1.2, -1);
        s.burst(
                4,
                5,
                "Three invisible frames (wall, floor, ceiling): each shows only its eye, no wood or backing, and in at least one shot an eye blinks.");
    }

    /** In-game: change a frame's contents several times. */
    private static void frameSwaps(Scene s) {
        s.ambient(20, "blink", "side_eye");
        String frame = s.frame(0, 1, -3, "south", RED_NBT, 0, false, false);
        s.cameraAt(0, 1.5, 0, 0, 1.5, -3);
        s.shot("A red-irised eye in a frame.");
        String[] items = {
            Scene.googlyNbt("{irisColor:" + Scene.BLUE + "}"),
            "{id:\"" + Scene.SLIMY + "\",count:1}",
            "{id:\"minecraft:diamond\",count:1}"
        };
        String[] expects = {
            "Now a blue-irised eye: no red pupil or old expression remains.",
            "Now a Slimy Eye: shown as its ordinary item, with no animated pupil.",
            "Now a diamond: an ordinary diamond, no pupil."
        };
        for (int i = 0; i < items.length; i++) {
            s.server("data merge entity %s {Item:%s}", Scene.sel(frame), items[i]);
            s.waitTicks(3);
            s.shot(expects[i]);
        }
        s.server("data remove entity %s Item", Scene.sel(frame));
        s.waitTicks(3);
        s.shot("An empty frame: nothing drawn in it.");
        s.server("kill %s", Scene.sel(frame));
        s.frame(0, 1, -3, "south", RED_NBT, 0, false, false);
        s.waitTicks(3);
        s.shot(
                "The frame broken and replaced with the red eye: a red-irised eye with its own fresh animation.");
    }

    /**
     * In-game: replace a framed eyed head with another species, another appearance, a plain head,
     * and a loose eye.
     */
    private static void framedHeadSwaps(Scene s) {
        String frame = framedHead(s, "minecraft:zombie_head[rotation=0]", 0, 1, -3, "south", 0);
        s.cameraAt(0, 1.5, 0, 0, 1.5, -3);
        s.shot("A framed zombie head with eyes.");
        String[][] replacements = {
            {
                "skeleton",
                "Now an eyed skeleton skull: only the skeleton layout, nothing left of the zombie's eyes."
            },
            {
                "red zombie",
                "Now a zombie head with red-irised eyes: the red appearance, no white irises left."
            },
            {"plain", "Now a plain skeleton skull: no eyes at all."},
            {"loose", "Now a loose Googly Eye: one large framed eye, no head layout."}
        };
        for (String[] replacement : replacements) {
            s.cameraOn(0, 1, -1, frame);
            s.attack();
            switch (replacement[0]) {
                case "skeleton" -> s.eyedHeadItem("minecraft:skeleton_skull[rotation=0]");
                case "red zombie" ->
                        s.eyedHeadItem(
                                "minecraft:zombie_head[rotation=0]",
                                ApplyClientTests.slimy("{irisColor:" + Scene.RED + "}"));
                case "plain" -> {
                    s.clearInventory();
                    s.give("minecraft:skeleton_skull");
                }
                default -> {
                    s.clearInventory();
                    s.give(Scene.GOOGLY);
                }
            }
            s.hotbar(0);
            s.useHeldOn(frame, 0, 1, -1);
            s.cameraAt(0, 1.5, 0, 0, 1.5, -3);
            s.shot(replacement[1]);
        }
    }

    /**
     * In-game: identical eyes in hotbar, inventory, and chest slots, then in the creative
     * inventory.
     */
    private static void slotsIndependent(Scene s) {
        s.config(
                "ambientBehaviors",
                "true",
                "ambientMinTicks",
                "10",
                "ambientMaxTicks",
                "60",
                "ambientBehaviorPool",
                Scene.tomlList("blink"));
        s.gamemode("survival");
        for (int i = 0; i < 4; i++) {
            s.slot("hotbar." + i, RED);
            s.slot("inventory." + i, RED);
        }
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            items.append(i == 0 ? "" : ",")
                    .append("{Slot:")
                    .append(i)
                    .append("b,id:\"")
                    .append(Scene.GOOGLY)
                    .append("\",count:1,components:{\"somegoogly:eye_properties\":{irisColor:")
                    .append(Scene.RED)
                    .append("}}}");
        }
        s.setBlock(0, 0, 2, "minecraft:chest[facing=north]{Items:[" + items + "]}");
        s.cameraAt(0, 0, 0, 0, 0.5, 2);
        s.use();
        s.context.waitForScreen(ContainerScreen.class);
        s.burst(
                4,
                7,
                "A chest screen: identical red eyes in chest, inventory, and hotbar slots blink at random times; in at least one shot they are not all in the same state.");
        s.closeScreen();
        s.gamemode("creative");
        s.press(s.options().keyInventory);
        s.context.waitForScreen(CreativeModeInventoryScreen.class);
        selectTab(
                s, BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INVENTORY));
        s.burst(
                3,
                7,
                "The creative inventory tab: identical eyes in several slots are not all in the same state in at least one shot.");
    }

    private static void selectTab(Scene s, CreativeModeTab tab) {
        s.context.runOnClient(
                mc -> ((FabricCreativeInventoryScreen) mc.screen).setSelectedItemGroup(tab));
        s.waitTicks(5);
    }

    /**
     * In-game: move, split, merge, shift-click, and drag eye stacks, and swap one for another
     * color.
     */
    private static void movingStacks(Scene s) {
        s.ambient(20, "blink");
        s.gamemode("survival");
        s.clearInventory();
        s.give(RED, 16);
        s.press(s.options().keyInventory);
        s.context.waitForScreen(InventoryScreen.class);
        int hotbar = Scene.INVENTORY_HOTBAR_START;
        s.click(hotbar, 0, ClickType.PICKUP);
        s.input.setCursorPos(400, 120);
        s.waitTicks(3);
        s.shot("Picked up the stack: the eye on the cursor animates on its own.");
        s.click(9, 0, ClickType.PICKUP);
        s.shot(
                "Placed in the first inventory slot: the icon continues its animation; no restart flash.");
        s.click(9, 1, ClickType.PICKUP);
        s.click(10, 0, ClickType.PICKUP);
        s.shot(
                "Split in two (8 and 8): both icons animate; neither shows an old expression stuck on it.");
        s.click(10, 0, ClickType.PICKUP);
        s.click(9, 0, ClickType.PICKUP);
        s.shot("Merged back into one stack of 16: the icon keeps animating without restarting.");
        s.click(9, 0, ClickType.QUICK_MOVE);
        s.shot("Shift-clicked into the hotbar: the stack's icon animates in its new slot.");
        s.click(hotbar, 0, ClickType.PICKUP);
        s.click(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT);
        for (int slot : new int[] {20, 21, 22}) {
            s.click(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT);
        }
        s.click(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT);
        s.shot("Dragged across three slots: each gets part of the stack and animates.");
        s.slot("inventory.11", BLUE);
        s.waitTicks(3);
        s.shot("One of the dragged stacks replaced by a blue eye: it shows the blue iris at once.");
    }

    /**
     * In-game: a stack of 64 and single eyes in the hotbar, one selected, then hover one in the
     * inventory.
     */
    private static void overlays(Scene s) {
        s.ambient(20, "blink");
        s.gamemode("survival");
        s.clearInventory();
        s.give(RED, 64);
        s.slot("hotbar.1", RED);
        s.slot("hotbar.2", BLUE);
        s.hotbar(2);
        s.hud(true);
        s.burst(
                3,
                6,
                "Hotbar: a stack of 64 red eyes with its count, single red and blue eyes, the blue one selected. Icons are normal size; blinks stay inside each icon.");
        s.press(s.options().keyInventory);
        s.context.waitForScreen(InventoryScreen.class);
        s.hoverSlot(Scene.INVENTORY_HOTBAR_START, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        s.shot(
                "Inventory with the mouse over the 64-stack: the slot is highlighted and the Googly Eye tooltip shows.");
    }

    /**
     * In-game: scroll creative tabs, change containers, and reopen the world, checking icons each
     * time.
     */
    private static void viewChanges(Scene s) {
        s.ambient(20, "blink");
        s.press(s.options().keyInventory);
        s.context.waitForScreen(CreativeModeInventoryScreen.class);
        selectTab(s, ModContent.CREATIVE_TAB.get());
        s.shot("The Some Googly Eyes tab: Googly Eye and Slimy Eye icons as usual.");
        selectTab(
                s,
                BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(
                        CreativeModeTabs.BUILDING_BLOCKS));
        selectTab(s, ModContent.CREATIVE_TAB.get());
        s.shot("Back on the tab after visiting another: same icons, no leftover layout or colors.");
        s.closeScreen();
        s.setBlock(
                -1,
                0,
                2,
                "minecraft:chest[facing=north]{Items:[{Slot:0b,id:\""
                        + Scene.GOOGLY
                        + "\",count:1,components:{\"somegoogly:eye_properties\":{irisColor:"
                        + Scene.RED
                        + "}}}]}");
        s.setBlock(
                2,
                0,
                2,
                "minecraft:chest[facing=north]{Items:[{Slot:0b,id:\""
                        + Scene.GOOGLY
                        + "\",count:1,components:{\"somegoogly:eye_properties\":{irisColor:"
                        + Scene.BLUE
                        + "}}}]}");
        s.cameraAt(-1, 0, 0, -1, 0.5, 2);
        s.use();
        s.context.waitForScreen(ContainerScreen.class);
        s.shot("First chest: one red-irised eye.");
        s.closeScreen();
        s.cameraAt(2, 0, 0, 2, 0.5, 2);
        s.use();
        s.context.waitForScreen(ContainerScreen.class);
        s.shot("Second chest: one blue-irised eye; nothing red carried over from the first chest.");
        s.closeScreen();
        s.slot("hotbar.0", RED);
        s.reopen();
        s.hud(true);
        s.shot("After save and reopen: the hotbar's red eye shows red and animates.");
    }

    /** In-game: a hotbar with a plain head, a Slimy Eye, and a diamond. */
    private static void otherItems(Scene s) {
        s.clearInventory();
        s.slot("hotbar.0", "minecraft:zombie_head");
        s.slot("hotbar.1", Scene.SLIMY);
        s.slot("hotbar.2", "minecraft:diamond");
        s.hud(true);
        s.shot(
                "Hotbar: a plain zombie head, a Slimy Eye, and a diamond, each drawn as its ordinary icon.");
    }

    /** In-game: hover an eyed head item from a broken eyed head. */
    private static void headTooltip(Scene s) {
        s.eyedHeadItem("minecraft:zombie_head[rotation=0]");
        s.gamemode("survival");
        s.press(s.options().keyInventory);
        s.context.waitForScreen(InventoryScreen.class);
        s.hoverSlot(Scene.INVENTORY_HOTBAR_START, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        s.shot("The tooltip of a zombie head broken off with eyes lists its eyes.");
    }

    /** In-game: a still zombie holding an eye and an eyed head. */
    private static void mobHands(Scene s) {
        s.ambient(20, "blink");
        String zombie = s.mob("minecraft:zombie", 0, 0, 0);
        s.eyedHeadItem("minecraft:zombie_head[rotation=0]");
        s.server(
                "item replace entity %s weapon.offhand from entity @p hotbar.0", Scene.sel(zombie));
        s.server("item replace entity %s weapon.mainhand with %s", Scene.sel(zombie), RED);
        s.cameraOn(0, 1.4, 2.5, zombie);
        s.burst(
                4,
                5,
                "A zombie holding a red eye and an eyed zombie head: both animate, and not always in step.");
    }

    /** In-game: in F5, change what the hands hold in several ways. */
    private static void handChanges(Scene s) {
        s.ambient(20, "blink");
        s.clearInventory();
        s.give(RED, 4);
        s.slot("hotbar.1", BLUE);
        s.hotbar(0);
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("F5 front holding red eyes in the main hand.");
        s.press(s.options().keySwapOffhand);
        s.shot(
                "Swapped to the offhand: the red eyes are in the other hand; the main hand is empty.");
        s.press(s.options().keySwapOffhand);
        s.hotbar(1);
        s.shot("Selected the blue eye: the main hand shows blue, nothing red left.");
        s.hotbar(0);
        s.give(RED, 4);
        s.shot("Count changed from 4 to 8: the red eye is still shown, animating normally.");
        s.slot("weapon.mainhand", BLUE);
        s.shot("Replaced by a blue eye: blue at once.");
        s.clearInventory();
        s.shot("Hands empty: no eye drawn.");
        s.slot("weapon.mainhand", RED);
        s.camera(0, 0, 400, 0, 0);
        s.waitTicks(20);
        s.camera(0, 0, 0, 0, 0);
        s.waitTicks(5);
        s.shot("Back after leaving the area: the main hand shows the red eye.");
        s.reopen();
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("After save and reopen: the main hand shows the red eye.");
    }

    /** In-game: hold an eyed head in first person and walk. */
    private static void firstPersonHead(Scene s) {
        s.eyedHeadItem("minecraft:zombie_head[rotation=0]");
        s.hotbar(0);
        s.gamemode("survival");
        s.camera(0, 0, 0, 0, 0);
        s.hold(s.options().keyUp);
        s.waitTicks(10);
        s.shot(
                "First person walking with an eyed zombie head in hand: its pupils rest centered and still.");
        s.release(s.options().keyUp);
    }

    /** In-game: drop an eye and an eyed head on the ground. */
    private static void ground(Scene s) {
        s.ambient(20, "blink");
        s.server(
                "summon minecraft:item %s %d %s {Item:%s,PickupDelay:32767}",
                String.valueOf(s.x(-1)), Scene.GROUND, String.valueOf(s.z(0)), RED_NBT);
        s.eyedHeadItem("minecraft:zombie_head[rotation=0]");
        s.camera(1, 0, 0, 180, 0);
        s.press(s.options().keyDrop);
        s.camera(0, 1.2, 3, 180, 20);
        s.waitTicks(20);
        s.shot("A red eye and an eyed zombie head lying on the ground: pupils centered.");
        s.waitTicks(20);
        s.shot("Twenty ticks later: the same, pupils centered and unchanged.");
    }

    /**
     * In-game: matching eyes in a frame, the hotbar, both hands, and on the ground; toggle first
     * and third person.
     */
    private static void isolation(Scene s) {
        s.ambient(20, "blink", "side_eye");
        s.frame(0, 1, -3, "south", RED_NBT, 0, false, false);
        s.server(
                "summon minecraft:item %s %d %s {Item:%s,PickupDelay:32767}",
                String.valueOf(s.x(1)), Scene.GROUND, String.valueOf(s.z(-1)), RED_NBT);
        s.clearInventory();
        s.give(RED, 2);
        s.slot("weapon.offhand", RED);
        s.hud(true);
        s.camera(0, 0, 0, 180, 10);
        for (int i = 0; i < 2; i++) {
            s.view(CameraType.THIRD_PERSON_BACK);
            s.shot(
                    "Third person: the framed eye, hotbar eye, both held eyes, and the ground eye each in its own state; the ground eye centered.");
            s.view(CameraType.FIRST_PERSON);
            s.shot(
                    "First person: the held eye, hotbar eye, and framed eye each in its own state; no world eye mirrors the hotbar icon.");
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario(
                            "8.1.1",
                            "Framed eye appearance and lighting",
                            () -> frameAppearance(s));
                    s.scenario("8.1.2", "Invisible frames", () -> invisibleFrames(s));
                    s.scenario("8.1.3", "Frame contents swaps", () -> frameSwaps(s));
                    s.scenario("8.1.4", "Eyed heads in frames", () -> framedHeads(s));
                    s.scenario("8.1.5", "Framed head replacement", () -> framedHeadSwaps(s));
                    s.scenario("8.2.1", "Slots animate independently", () -> slotsIndependent(s));
                    s.scenario("8.2.2", "Moving stacks", () -> movingStacks(s));
                    s.scenario("8.2.3", "Icon size and overlays", () -> overlays(s));
                    s.scenario("8.2.4", "View changes leak nothing", () -> viewChanges(s));
                    s.scenario("8.2.5", "Other items unchanged", () -> otherItems(s));
                    s.scenario("8.2.6", "Head item tooltip", () -> headTooltip(s));
                    s.scenario("8.3.1", "Mob-held eyes", () -> mobHands(s));
                    s.scenario("8.3.2", "Hand changes", () -> handChanges(s));
                    s.scenario("8.3.3", "First-person head item", () -> firstPersonHead(s));
                    s.scenario("8.4.1", "Ground items", () -> ground(s));
                    s.scenario("8.5.1", "Context isolation", () -> isolation(s));
                });
    }
}
