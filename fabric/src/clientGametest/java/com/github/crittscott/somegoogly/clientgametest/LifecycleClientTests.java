package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.WinScreen;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client test plan section 10: spawning, conversion, death, dimensions, saving, and opening older
 * worlds.
 */
public final class LifecycleClientTests implements FabricClientGameTest {
    /** The owner-supplied world saved by the 1.21.4 release, under the fixtures folder. */
    static final String WORLD_1214 = "world-1.21.4";

    private static final String RED = Scene.googly("{irisColor:" + Scene.RED + "}");

    /** In-game: summon five cows at spawn chance 100 and five at 0. */
    private static void spawnChance(Scene s) {
        s.wideView();
        s.config("globalPercent", "100");
        for (int i = 0; i < 5; i++) {
            s.mob("minecraft:cow", -6 + i * 3, 0, -2, 0, "");
        }
        s.config("globalPercent", "0");
        for (int i = 0; i < 5; i++) {
            s.mob("minecraft:cow", -6 + i * 3, 0, 2, 0, "");
        }
        s.cameraAt(0, 5, 9, 0, 0, 0);
        s.shot(
                "Back row (spawned at 100 percent): all five cows have eyes. Front row (0 percent): none has eyes.");
    }

    /** In-game: with overrides, summon a cow, a chicken, and a pig. */
    private static void overrides(Scene s) {
        s.config(
                "globalPercent",
                "0",
                "entityOverrides",
                Scene.tomlList("minecraft:cow,100", "minecraft:c*,0", "minecraft:*,100"));
        s.mob("minecraft:cow", -3, 0, 0, 0, "");
        s.mob("minecraft:chicken", 0, 0, 0, 0, "");
        s.mob("minecraft:pig", 3, 0, 0, 0, "");
        s.cameraAt(0, 1.5, 5, 0, 0.7, 0);
        s.shot(
                "Overrides cow 100, c* 0, * 100. Left cow: eyes (exact id). Middle chicken: none (first matching wildcard). Right pig: eyes (second wildcard).");
    }

    /** In-game: at spawn chance 100, rejoin, then give yourself eyes, die, and respawn. */
    private static void playerNatural(Scene s) {
        s.config("globalPercent", "100");
        s.reopen();
        s.area();
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("After rejoining at spawn chance 100: the player has no eyes.");
        s.view(CameraType.FIRST_PERSON);
        s.useOnSelf(Scene.SLIMY);
        s.server("kill @p");
        s.context.waitForScreen(DeathScreen.class);
        s.context.runOnClient(mc -> mc.player.respawn());
        s.waitTicks(20);
        s.area();
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("After dying with eyes and respawning: the player has no eyes.");
    }

    /**
     * In-game: cure an eyed zombie villager, strike an eyed villager with lightning, and drown an
     * eyed zombie.
     */
    private static void conversions(Scene s) {
        String zombieVillager = s.mob("minecraft:zombie_villager", -4, 0, 0);
        s.eyes(zombieVillager);
        s.server("effect give %s minecraft:weakness 60 0", Scene.sel(zombieVillager));
        s.useOnEntity(zombieVillager, -4, 0, 2.5, "minecraft:golden_apple");
        s.server("data merge entity %s {ConversionTime:20}", Scene.sel(zombieVillager));
        String villager = s.mob("minecraft:villager", 0, 0, 0);
        s.eyes(villager);
        s.server(
                "summon minecraft:lightning_bolt %s %d %s",
                String.valueOf(s.x(0)), Scene.GROUND, String.valueOf(s.z(0)));
        s.fill(3, -3, -1, 5, -1, 1, "minecraft:water");
        String zombie = s.mob("minecraft:zombie", 4, -2, 0, 0, "DrownedConversionTime:20");
        s.eyes(zombie);
        s.waitTicks(80);
        s.cameraAt(0, 2, 6, 0, 0.5, 0);
        s.shot(
                "Left: the cured zombie villager, now a villager, with eyes. Middle: the villager struck by lightning, now a witch, with eyes. Right (in water): the zombie, now a drowned, with eyes.");
    }

    /** In-game: with eyes, travel to the End and leave through an exit portal. */
    private static void leaveEnd(Scene s) {
        s.useOnSelf(Scene.SLIMY);
        s.server("execute in minecraft:the_end run tp @p 0 100 0");
        s.waitTicks(40);
        s.server("execute in minecraft:the_end run setblock 0 100 0 minecraft:end_portal");
        s.context.waitForScreen(WinScreen.class);
        s.context.runOnClient(mc -> mc.screen.onClose());
        s.waitTicks(40);
        s.area();
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("Back in the Overworld after leaving the End: the player still has eyes.");
    }

    /** Builds the persistence scene and shoots it. */
    private static void persistenceShots(Scene s, String when) {
        s.camera(0, 0, 4, 180, 10);
        s.view(CameraType.FIRST_PERSON);
        s.shot(
                when
                        + ": a red-irised cow, an eyed zombie head, an eyed boat, a framed blue eye, and a red eye in the hotbar.");
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot(when + ": the player has eyes.");
        s.view(CameraType.FIRST_PERSON);
    }

    /** In-game: a scene of eyed things, saved and reopened, and left and returned to. */
    private static void persistence(Scene s) {
        String cow = s.mob("minecraft:cow", -3, 0, 0);
        s.eyes(cow, "tint iris 1 0 0");
        s.eyedHead("minecraft:zombie_head[rotation=0]", 0, 0, 0);
        String boat = s.boat("minecraft:oak_boat", 3, 0, 90);
        s.useOnEntity(boat, 3, 2.5, 2.5, Scene.SLIMY);
        s.frame(
                0,
                2,
                -3,
                "south",
                Scene.googlyNbt("{irisColor:" + Scene.BLUE + "}"),
                0,
                false,
                false);
        s.useOnSelf(Scene.SLIMY);
        s.clearInventory();
        s.give(RED);
        s.hotbar(0);
        s.hud(true);
        persistenceShots(s, "Before saving");
        s.reopen();
        s.hud(true);
        persistenceShots(s, "After save and reopen, the same");
        s.camera(0, 0, 800, 0, 0);
        s.waitTicks(100);
        persistenceShots(s, "After going far enough to unload the area and coming back, the same");
    }

    /**
     * In-game: break an eyed head and place it again; let a charged creeper kill an eyed zombie and
     * place its head.
     */
    private static void headItems(Scene s) {
        s.eyedHeadItem("minecraft:zombie_head[rotation=0]");
        s.hotbar(0);
        s.cameraAt(-2, 0, 2, -2, -0.5, 0);
        s.use();
        s.cameraAt(-2, 1.2, 2.5, -2, 0.3, 0);
        s.shot("A zombie head that was broken off with eyes and placed again: its eyes are back.");
        String zombie = s.mob("minecraft:zombie", 3, 0, 0);
        s.eyes(zombie);
        s.mob("minecraft:creeper", 4.2, 0, 0, 0, "powered:1b,Fuse:1,ignited:1b");
        s.waitTicks(20);
        s.clearInventory();
        s.camera(3, 0, 0, 0, 90);
        s.waitTicks(15);
        s.hotbar(0);
        s.cameraAt(3, 0, 2, 3, -0.5, 0);
        s.use();
        s.cameraAt(3, 1.2, 2.5, 3, 0.3, 0);
        s.shot(
                "The head an eyed zombie dropped when a charged creeper killed it, placed on the ground: it has eyes.");
    }

    /** In-game: break eyed boats in survival and creative, then place the dropped boats. */
    private static void boatItems(Scene s) {
        s.water(-7, -2, 7, 3);
        String[] types = {
            "minecraft:oak_boat",
            "minecraft:oak_chest_boat",
            "minecraft:bamboo_raft",
            "minecraft:spruce_boat"
        };
        String[] tags = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            tags[i] = s.boat(types[i], -5 + i * 3.3, 0, 90);
            s.useOnEntity(tags[i], -5 + i * 3.3, 2.5, 2.5, Scene.SLIMY);
        }
        s.gamemode("survival");
        for (int i = 0; i < 3; i++) {
            for (int hit = 0; hit < 6; hit++) {
                s.cameraOn(-5 + i * 3.3, 0.5, 2.5, tags[i]);
                s.attack();
                s.waitTicks(3);
            }
        }
        s.gamemode("creative");
        s.cameraOn(-5 + 3 * 3.3, 0.5, 2.5, tags[3]);
        s.attack();
        s.cameraAt(0, 4, 6, 0, 0, 0);
        s.waitTicks(10);
        s.shot(
                "Broken eyed boats: the oak boat, chest boat (with its chest), and raft dropped only their boat items, no Googly Eye. The spruce boat broken in creative dropped nothing.");
        s.clearInventory();
        s.give("minecraft:oak_boat");
        s.hotbar(0);
        s.cameraAt(0, 2, 4, 0, -1, 1);
        s.use();
        s.cameraAt(4, 1, 1, 0, 0, 1);
        s.shot("A boat placed from a boat item, seen from its side: no eyes.");
    }

    /**
     * In-game: with eyed things in view, visit the Nether and return, then open a different world.
     */
    private static void worldChanges(Scene s) {
        String cow = s.mob("minecraft:cow", -2, 0, 0);
        s.eyes(cow);
        s.frame(
                0,
                2,
                -3,
                "south",
                Scene.googlyNbt("{irisColor:" + Scene.RED + "}"),
                0,
                false,
                false);
        s.water(1, -1, 4, 1);
        String boat = s.boat("minecraft:oak_boat", 2.5, 0, 90);
        s.useOnEntity(boat, 2.5, 2.5, 2.5, Scene.SLIMY);
        s.clearInventory();
        s.give(RED);
        s.hotbar(0);
        s.server("execute in minecraft:the_nether run tp @p 0 120 0");
        s.waitTicks(60);
        s.server(
                "execute in minecraft:overworld run tp @p %s %d %s 180 10",
                String.valueOf(s.x(0)), Scene.GROUND, String.valueOf(s.z(5)));
        s.waitTicks(60);
        s.view(CameraType.THIRD_PERSON_BACK);
        s.shot(
                "Back from the Nether: the cow, framed eye, boat, and held eye show their correct eyes.");
        s.view(CameraType.FIRST_PERSON);
        s.newWorld(true);
        s.area();
        s.camera(0, 0, 5, 180, 10);
        s.shot("A different world at a fresh spot: no leftover eyes or eye shapes anywhere.");
    }

    /**
     * In-game: open a world saved by the 1.21.4 release that has an eyed mob, eye items, and
     * changed settings.
     */
    private static void olderWorld(Scene s) {
        Path fixture =
                Scene.fixtures()
                        .map(path -> path.resolve(WORLD_1214))
                        .filter(Files::isDirectory)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No "
                                                + WORLD_1214
                                                + " fixture; see "
                                                + "fabric/src/clientGametest/fixtures/README.md"));
        s.reopenFrom(fixture);
        s.hud(true);
        s.shot(
                "The 1.21.4 world, as its player left it: the eyed mob in view keeps its eyes and colors, and the hotbar eye items are intact.");
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("The 1.21.4 world in F5 front: as the fixture's README describes.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("10.1", "Global spawn chance", () -> spawnChance(s));
                    s.scenario("10.2", "Entity overrides", () -> overrides(s));
                    s.scenario("10.4", "Players never roll; death clears", () -> playerNatural(s));
                    s.scenario("10.5", "Conversions keep eyes", () -> conversions(s));
                    s.scenario("10.6", "Leaving the End", () -> leaveEnd(s));
                    s.scenario("10.7", "Save, reload, and unload", () -> persistence(s));
                    s.scenario("10.8", "Head items keep eyes", () -> headItems(s));
                    s.scenario("10.9", "Boat items carry no eyes", () -> boatItems(s));
                    s.scenario("10.11", "Opening a 1.21.4 world", () -> olderWorld(s));
                    // Last: it moves the class to a new world.
                    s.scenario("10.10", "World and dimension changes", () -> worldChanges(s));
                });
    }
}
