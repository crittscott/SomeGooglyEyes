package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Client test plan section 9: the sneak-and-aim target check. Shots show the HUD, where the verdict
 * appears.
 */
public final class TargetCheckClientTests implements FabricClientGameTest {
    /**
     * Sneaks while holding {@code item}, aimed at the tagged entity from a spot, and shoots the
     * verdict.
     */
    static void inspect(
            Scene s, String tag, double dx, double dy, double dz, String item, String expect) {
        s.clearInventory();
        s.give(item);
        s.hotbar(0);
        s.cameraOn(dx, dy, dz, tag);
        inspectHere(s, expect);
    }

    /** Sneaks with whatever is held, aimed wherever the camera points, and shoots the verdict. */
    static void inspectHere(Scene s, String expect) {
        s.hud(true);
        s.hold(s.options().keyShift);
        s.waitTicks(5);
        s.shot(expect);
        s.release(s.options().keyShift);
        s.waitTicks(20);
    }

    /** In-game: sneak-aim a Googly Eye at four mobs, and a Slimy Eye at one. */
    private static void verdicts(Scene s) {
        String eyeless = s.mob("minecraft:cow", -4.5, 0, 0);
        String eyed = s.mob("minecraft:cow", -1.5, 0, 0);
        s.eyes(eyed);
        String baby = s.mob("minecraft:armadillo", 1.5, 0, 0, 0, "Age:-24000");
        String none = s.mob("minecraft:silverfish", 4.5, 0, 0);
        inspect(
                s,
                eyeless,
                -4.5,
                0,
                2.5,
                Scene.GOOGLY,
                "Aiming at an eyeless cow: the check says it can receive eyes now.");
        inspect(
                s,
                eyed,
                -1.5,
                0,
                2.5,
                Scene.GOOGLY,
                "Aiming at an eyed cow: the check says it already has eyes.");
        inspect(
                s,
                baby,
                1.5,
                0,
                2,
                Scene.GOOGLY,
                "Aiming at a baby armadillo: the check says eyes are supported only at another life stage.");
        inspect(
                s,
                none,
                4.5,
                0,
                2,
                Scene.GOOGLY,
                "Aiming at a silverfish: the check says it has no configuration.");
        inspect(
                s,
                eyeless,
                -4.5,
                0,
                2.5,
                Scene.SLIMY,
                "Holding a Slimy Eye at the eyeless cow: the same \"can receive eyes now\" verdict.");
    }

    /** In-game: sneak-aim at an eyeless boat and an eyed placed head. */
    private static void boatAndHead(Scene s) {
        s.water(-5, -2, -1, 2);
        String boat = s.boat("minecraft:oak_boat", -3, 0, 90);
        s.eyedHead("minecraft:zombie_head[rotation=0]", 3, 0, 0);
        inspect(
                s,
                boat,
                -3,
                1,
                2.5,
                Scene.GOOGLY,
                "Aiming at an eyeless boat: the check says it can receive eyes now.");
        s.cameraAt(3, 0.5, 2.5, 3, 0.25, 0);
        inspectHere(s, "Aiming at an eyed zombie head: the check says it already has eyes.");
    }

    /** In-game: sneak-aim at a cow behind a wall, and at one just beyond reach. */
    private static void blocked(Scene s) {
        String walled = s.mob("minecraft:cow", -3, 0, 0);
        s.fill(-5, 0, 2, -1, 3, 2, "minecraft:stone");
        String far = s.mob("minecraft:cow", 4, 0, 0);
        inspect(
                s,
                walled,
                -3,
                0,
                4,
                Scene.GOOGLY,
                "Aiming at a cow behind a stone wall: no verdict is shown.");
        inspect(
                s,
                far,
                4,
                0,
                8,
                Scene.GOOGLY,
                "Aiming at a cow eight blocks away, beyond reach: no verdict is shown.");
    }

    /**
     * In-game: check an eyeless cow, disable the cow definition with a datapack, and check again.
     */
    private static void reloadChangesVerdict(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        inspect(s, cow, 0, 0, 2.5, Scene.GOOGLY, "Before: the eyeless cow can receive eyes now.");
        s.datapack(TestPacks.COW_OFF);
        inspect(
                s,
                cow,
                0,
                0,
                2.5,
                Scene.GOOGLY,
                "After a datapack disabled the cow definition and reloaded: the check says the cow has no configuration.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("9.1", "Each verdict", () -> verdicts(s));
                    s.scenario("9.2", "Boats and heads", () -> boatAndHead(s));
                    s.scenario("9.3", "Walls and reach", () -> blocked(s));
                    s.scenario("9.5", "Reload changes the verdict", () -> reloadChangesVerdict(s));
                });
    }
}
