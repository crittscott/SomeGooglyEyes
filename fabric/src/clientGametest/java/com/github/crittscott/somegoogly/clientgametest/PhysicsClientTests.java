package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.HumanoidArm;

/** Client test plan section 2: pupil physics. Ambient expressions are off throughout, so only physics moves pupils. */
public final class PhysicsClientTests implements FabricClientGameTest {
    private static final String RED_EYE = Scene.googly("{irisColor:" + Scene.RED + "}");

    /**
     * In-game: row an eyed boat forward, back, and turning while a friend watches from the side; hit it; float it on
     * bubble columns. Here the boat is pushed and turned by commands so a fixed camera can watch the eyed side.
     */
    private static void boatMotion(Scene s) {
        s.water(-11, -4, 11, 4);
        String boat = s.boat("minecraft:oak_boat", 0, 0, 90);
        s.useOnEntity(boat, 0, 2.5, 2.5, Scene.SLIMY);
        s.camera(0, 1, 7, 180, 5);
        s.shot("A still eyed boat seen from its side: pupils hang toward world down.");
        s.server("data merge entity %s {Motion:[-0.8d,0d,0d]}", Scene.sel(boat));
        s.waitTicks(2);
        s.shot("Just pushed bow-first (to the left): the eye stays on the hull; its pupil lags toward the stern.");
        s.waitTicks(40);
        s.server("data merge entity %s {Motion:[0.8d,0d,0d]}", Scene.sel(boat));
        s.waitTicks(2);
        s.shot("Just pushed stern-first (to the right): the pupil lags toward the bow.");
        s.waitTicks(40);
        for (int i = 0; i < 6; i++) {
            s.server("execute as %s at @s run tp @s ~ ~ ~ ~8 ~", Scene.sel(boat));
            s.waitTicks(1);
        }
        s.shot("Turning: the eye stays on the hull; its pupil swings outward from the turn.");
        s.gamemode("survival");
        s.cameraOn(0, 0.5, 2.5, boat);
        s.attack();
        s.cameraAt(0, 1, 4, 0, 0, 0);
        s.shot("Just after a hit, seen from the side: the hull rocks; the eye stays attached and its pupil still hangs toward world down.");
        s.gamemode("creative");
        s.server("kill %s", Scene.sel(boat));
        for (String floor : new String[] {"minecraft:soul_sand", "minecraft:magma_block"}) {
            s.fill(-1, -4, -1, 1, -1, 1, "minecraft:water");
            s.fill(-1, -5, -1, 1, -5, 1, floor);
            s.waitTicks(10);
            String columnBoat = s.boat("minecraft:oak_boat", 0, 0, 90);
            s.useOnEntity(columnBoat, 0, 2.5, 2.5, Scene.SLIMY);
            s.waitTicks(10);
            s.cameraAt(0, 0.5, 5, 0, -0.5, 0);
            s.shot(floor.endsWith("soul_sand")
                    ? "A boat in an upward bubble column: eyes stay on the bobbing hull; pupils hang toward world down."
                    : "A boat over a downward bubble column: eyes stay on the hull as it is pulled down.");
            s.server("kill %s", Scene.sel(columnBoat));
        }
    }

    /** In-game: with a still scene, look level, up, down, and in F5; pupils keep the same world direction. */
    private static void cameraIndependence(Scene s) {
        String cow = s.mob("minecraft:cow", -2, 0, 0);
        s.eyes(cow);
        s.water(1, -1, 4, 1);
        String boat = s.boat("minecraft:oak_boat", 2.5, 0, 90);
        s.useOnEntity(boat, 2.5, 2.5, 2.5, Scene.SLIMY);
        s.frame(0, 2, -2, "south", Scene.googlyNbt("{}"), 0, false, false);
        s.clearInventory();
        s.give(RED_EYE);
        s.hotbar(0);
        s.waitTicks(40);
        s.camera(0, 1, 5, 180, 0);
        s.shot("Level view of a cow, a framed eye, and an eyed boat, with a held eye: all pupils hang toward world down.");
        s.camera(0, 1, 5, 180, -40);
        s.shot("Looking up at the same scene: every pupil points the same way relative to the world as in the level shot.");
        s.camera(0, 1, 5, 180, 40);
        s.shot("Looking down at the same scene: every pupil points the same way relative to the world as in the level shot.");
        s.view(CameraType.THIRD_PERSON_BACK);
        s.shot("F5 behind the player: pupils unchanged relative to the world, including the eye in the player's hand.");
    }

    /** In-game: place eyed heads on the floor facing each direction and on walls. */
    private static void headGravity(Scene s) {
        int[] rotations = {0, 4, 8, 12};
        for (int i = 0; i < rotations.length; i++) {
            s.eyedHead("minecraft:zombie_head[rotation=" + rotations[i] + "]", -3 + i * 2, 0, 0);
        }
        String[] facings = {"north", "east", "south", "west"};
        for (int i = 0; i < facings.length; i++) {
            int dx = -3 + i * 2;
            s.setBlock(dx, 1, -3, "minecraft:stone");
            s.eyedHead("minecraft:zombie_wall_head[facing=south]", dx, 1, -2);
        }
        s.waitTicks(40);
        s.cameraAt(0, 2.5, 5, 0, 0.5, -1);
        s.shot("Floor heads turned four ways and wall heads: every pupil has settled toward world down.");
    }

    /** In-game: open the inventory with eyes in several slots, at two GUI scales and a resized window. */
    private static void guiPose(Scene s) {
        s.gamemode("survival");
        for (String slot : new String[] {"hotbar.0", "hotbar.4", "inventory.0", "inventory.13", "inventory.26"}) {
            s.slot(slot, RED_EYE);
        }
        s.guiScale(2);
        s.press(s.options().keyInventory);
        s.context.waitForScreen(InventoryScreen.class);
        s.waitTicks(20);
        s.shot("Inventory at GUI scale 2: in every eye icon the pupil sits at the bottom of the icon.");
        s.guiScale(4);
        s.waitTicks(10);
        s.shot("Inventory at GUI scale 4: pupils still at the bottom of each icon.");
        int[] size = s.context.computeOnClient(mc -> new int[] {mc.getWindow().getWidth(), mc.getWindow().getHeight()});
        s.input.resizeWindow(1280, 720);
        s.waitTicks(10);
        s.shot("After resizing the window: pupils still at the bottom of each icon.");
        s.input.resizeWindow(size[0], size[1]);
    }

    /** In-game: hold an eye in first person, walk forward, and stop. */
    private static void heldEyeMotion(Scene s) {
        s.gamemode("survival");
        s.clearInventory();
        s.give(RED_EYE);
        s.hotbar(0);
        s.camera(0, 0, 0, 0, 0);
        s.waitTicks(30);
        s.shot("First person holding an eye at rest: the pupil hangs at the bottom.");
        s.hold(s.options().keyUp);
        s.waitTicks(15);
        s.release(s.options().keyUp);
        s.waitTicks(1);
        s.shot("Just after stopping a walk: the pupil has swung forward from rest.");
    }

    /** In-game: give a cow eyes, then move it sideways and turn it. */
    private static void mobMotion(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.cameraAt(0, 1.2, 5, 0, 1, 0);
        s.waitTicks(30);
        s.shot("A still cow: pupils settled at the bottom of each eye.");
        for (int i = 0; i < 4; i++) {
            s.server("execute as %s at @s run tp @s ~0.4 ~ ~", Scene.sel(cow));
            s.waitTicks(1);
        }
        s.shot("The cow just moved sideways: pupils trail opposite the motion.");
        s.waitTicks(30);
        s.server("execute as %s at @s run tp @s ~ ~ ~ ~50 ~", Scene.sel(cow));
        s.waitTicks(2);
        s.shot("The cow just turned its head: pupils swing with the turn.");
    }

    /** In-game: hold eyes in both hands in F5 while still, swinging, sneaking, walking, swimming, and left-handed. */
    private static void handMotion(Scene s) {
        s.clearInventory();
        s.give(RED_EYE);
        s.hotbar(0);
        s.slot("weapon.offhand", RED_EYE);
        s.gamemode("survival");
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.waitTicks(30);
        s.shot("F5 front, still, an eye in each hand: pupils toward world down.");
        s.attack();
        s.shot("Mid-swing: the main-hand pupil lags the swing.");
        s.hold(s.options().keyShift);
        s.waitTicks(5);
        s.shot("Sneaking: pupils toward world down for the tilted hands.");
        s.release(s.options().keyShift);
        s.hold(s.options().keyUp);
        s.waitTicks(10);
        s.shot("Walking: pupils lag the motion.");
        s.release(s.options().keyUp);
        s.view(CameraType.THIRD_PERSON_BACK);
        s.shot("F5 back: pupils toward world down.");
        s.fill(-2, -3, 4, 2, -1, 8, "minecraft:water");
        s.camera(0, -1.5, 6, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.waitTicks(10);
        s.shot("In water: both hand eyes animate; pupils toward world down.");
        s.camera(0, 0, 0, 0, 0);
        s.mainArm(HumanoidArm.LEFT);
        s.waitTicks(10);
        s.shot("Left-handed: the hands swap sides; pupils toward world down in both.");
        s.camera(0, 0, 0, 0, 89);
        s.shot("Looking straight down: the held eyes still render; pupil direction follows world down.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(context, Scene.MAIN, s -> {
            s.scenario("2.1", "Mob pupils settle and lag", () -> {
                s.config("ambientBehaviors", "false");
                mobMotion(s);
            });
            s.scenario("2.2", "Camera never moves gravity", () -> {
                s.config("ambientBehaviors", "false");
                cameraIndependence(s);
            });
            s.scenario("2.3", "Boat pupils in motion", () -> {
                s.config("ambientBehaviors", "false");
                boatMotion(s);
            });
            for (String facing : new String[] {"north", "south", "east", "west", "up", "down"}) {
                s.scenario("2.4", "Framed eyes facing " + facing, () -> {
                    s.config("ambientBehaviors", "false");
                    framedRow(s, facing);
                });
            }
            s.scenario("2.5", "Third-person hand pupils", () -> {
                s.config("ambientBehaviors", "false");
                handMotion(s);
            });
            s.scenario("2.6", "GUI pupils follow the icon", () -> {
                s.config("ambientBehaviors", "false");
                guiPose(s);
            });
            s.scenario("2.7", "First-person held eye", () -> {
                s.config("ambientBehaviors", "false");
                heldEyeMotion(s);
            });
            s.scenario("2.8", "Placed head gravity", () -> {
                s.config("ambientBehaviors", "false");
                headGravity(s);
            });
        });
    }

    /** In-game: a row of frames facing one way, each eye at a different item rotation. */
    private static void framedRow(Scene s, String facing) {
        s.wideView();
        boolean wall = !facing.equals("up") && !facing.equals("down");
        for (int rotation = 0; rotation < 8; rotation++) {
            int along = -7 + rotation * 2;
            int dy = facing.equals("down") ? 4 : facing.equals("up") ? 0 : 2;
            boolean alongX = facing.equals("north") || facing.equals("south") || !wall;
            s.frame(alongX ? along : 0, dy, alongX ? 0 : along, facing, Scene.googlyNbt("{irisColor:" + Scene.RED + "}"),
                    rotation, false, false);
        }
        s.waitTicks(40);
        switch (facing) {
            case "north" -> s.cameraAt(0, 2.5, -8, 0, 2.5, 0);
            case "south" -> s.cameraAt(0, 2.5, 8, 0, 2.5, 0);
            case "east" -> s.cameraAt(8, 2.5, 0, 0, 2.5, 0);
            case "west" -> s.cameraAt(-8, 2.5, 0, 0, 2.5, 0);
            case "up" -> s.cameraAt(0, 9, 0.1, 0, 0, 0);
            default -> s.cameraAt(0, 0.5, 0.1, 0, 4, 0);
        }
        s.shot(wall
                ? "Eight wall frames facing " + facing + ", each eye at a different rotation: every pupil sits at the bottom of its eye."
                : "Eight frames facing " + facing + ", each eye at a different rotation: pupil positions differ from frame to frame, with no shared direction.");
    }
}
