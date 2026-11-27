package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

/** Client test plan section 6: applying Slimy Eyes. Shots show the HUD where item counts matter. */
public final class ApplyClientTests implements FabricClientGameTest {
    static String slimy(String properties) {
        return Scene.SLIMY + "[somegoogly:eye_properties=" + properties + "]";
    }

    /** In-game: a green Slimy Eye on an eyeless cow in survival, then in creative. */
    private static void applyToMob(Scene s) {
        String green = slimy("{irisColor:" + Scene.GREEN + "}");
        String survivalCow = s.mob("minecraft:cow", -2, 0, 0);
        String creativeCow = s.mob("minecraft:cow", 2, 0, 0);
        s.gamemode("survival");
        s.hud(true);
        s.clearInventory();
        s.give(green, 2);
        s.hotbar(0);
        s.useHeldOn(survivalCow, -2, 1.6, 2.5);
        s.cameraOn(-2, 1.4, 3.5, survivalCow);
        s.shot(
                "Survival: the cow now has green-irised eyes, a squelch subtitle shows, and the hotbar stack went from 2 to 1.");
        s.gamemode("creative");
        s.hud(true);
        s.clearInventory();
        s.give(green, 2);
        s.hotbar(0);
        s.useHeldOn(creativeCow, 2, 1.6, 2.5);
        s.cameraOn(2, 1.4, 3.5, creativeCow);
        s.shot(
                "Creative: the second cow has green-irised eyes and the hotbar stack still shows 2.");
    }

    /** In-game: Slimy Eyes on each boat variant with different appearances and hands. */
    private static void boats(Scene s) {
        s.water(-7, -3, 7, 3);
        String oak = s.boat("minecraft:oak_boat", -5, 0, 90);
        String chest = s.boat("minecraft:oak_chest_boat", -1.7, 0, 90);
        String raft = s.boat("minecraft:bamboo_raft", 1.7, 0, 90);
        String chestRaft = s.boat("minecraft:bamboo_chest_raft", 5, 0, 90);
        s.gamemode("survival");
        s.hud(true);
        s.useOnEntity(oak, -5, 0.5, 2.5, Scene.SLIMY);
        s.clearInventory();
        s.slot("weapon.offhand", slimy("{irisColor:" + Scene.RED + "}"));
        s.cameraOn(-1.7, 0.5, 2.5, chest);
        s.aimAt(chest);
        s.hold(s.options().keyShift);
        s.use();
        s.release(s.options().keyShift);
        s.useOnEntity(raft, 1.7, 0.5, 2.5, slimy("{glow:1b}"));
        s.useOnEntity(chestRaft, 5, 0.5, 2.5, slimy("{glow:0b}"));
        s.server("time set midnight");
        s.cameraAt(0, 1.2, 7, 0, 0, 0);
        s.shot(
                "At night, left to right: oak boat with plain eyes (main hand), chest boat with red irises (offhand), raft with glowing eyes, chest raft with eyes not glowing. The player is not seated and no chest screen opened.");
        s.server("time set noon");
        s.clearInventory();
        s.give(Scene.SLIMY, 2);
        s.hotbar(0);
        s.useHeldOn(oak, -5, 0.5, 2.5);
        s.cameraAt(-5, 1, 3.5, -5, 0, 0);
        s.shot(
                "A Slimy Eye used on the already eyed oak boat: its eyes are unchanged and the hotbar stack still shows 2.");
    }

    /** In-game: a Slimy Eye on an eyeless head in survival, and on another in adventure mode. */
    private static void heads(Scene s) {
        s.setBlock(-2, 0, 0, "minecraft:zombie_head[rotation=0]");
        s.setBlock(2, 0, 0, "minecraft:zombie_head[rotation=0]");
        s.gamemode("survival");
        s.useOnBlock(-2, 0, 0, Scene.SLIMY);
        s.gamemode("adventure");
        s.useOnBlock(2, 0, 0, Scene.SLIMY);
        s.hud(true);
        s.cameraAt(0, 1.5, 3, 0, 0.3, 0);
        s.shot(
                "Left head (survival) has eyes. Right head (adventure mode) has none, and the Slimy Eye is still in the hotbar.");
    }

    /** In-game: a Slimy Eye on an eyed cow, a silverfish, and a baby armadillo, in survival. */
    private static void refusals(Scene s) {
        String eyed = s.mob("minecraft:cow", -3, 0, 0);
        s.eyes(eyed);
        String silverfish = s.mob("minecraft:silverfish", 0, 0, 0);
        String baby = s.mob("minecraft:armadillo", 3, 0, 0, 0, "Age:-24000");
        s.gamemode("survival");
        s.hud(true);
        s.useOnEntity(eyed, -3, 1.6, 2.5, Scene.SLIMY);
        s.cameraOn(-3, 1.4, 3.5, eyed);
        s.shot(
                "Already eyed cow: its eyes are unchanged and the Slimy Eye is still in the hotbar.");
        s.useOnEntity(silverfish, 0, 1, 2, Scene.SLIMY);
        s.cameraOn(0, 1, 2.5, silverfish);
        s.shot("Silverfish (no definition): no eyes, and the Slimy Eye is still in the hotbar.");
        s.useOnEntity(baby, 3, 1, 2, Scene.SLIMY);
        s.cameraOn(3, 1, 2.5, baby);
        s.shot(
                "Baby armadillo (adult-only definition): no eyes, and the Slimy Eye is still in the hotbar.");
    }

    /** In-game: sneak-use a Slimy Eye on the air. */
    private static void self(Scene s) {
        s.useOnSelf(Scene.SLIMY);
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("F5 front: the player's face now has eyes.");
    }

    /** In-game: a Slimy Eye used by a spectator on an eyeless cow. */
    private static void spectator(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.gamemode("spectator");
        s.useOnEntity(cow, 0, 1.6, 2.5, Scene.SLIMY);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot("A spectator used a Slimy Eye on the cow: it still has no eyes.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("6.1", "Slimy Eye on a mob", () -> applyToMob(s));
                    s.scenario("6.2", "Refused targets", () -> refusals(s));
                    s.scenario("6.3", "Slimy Eye on yourself", () -> self(s));
                    s.scenario("6.4", "Heads and build permission", () -> heads(s));
                    s.scenario("6.5", "Boats, both hands, appearances", () -> boats(s));
                    s.scenario("6.6", "Spectators cannot apply", () -> spectator(s));
                });
    }
}
