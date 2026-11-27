package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Client test plan section 3: ambient expressions. Pools and intervals are set through the server
 * config, so the expressions arrive the way they do in play; bursts of shots cover the moment an
 * expression lands in its slot.
 */
public final class AmbientClientTests implements FabricClientGameTest {
    private static final String EYE = Scene.googlyNbt("{irisColor:" + Scene.BLUE + "}");
    private static final String EYE_ITEM = Scene.googly("{irisColor:" + Scene.BLUE + "}");

    /** In-game: set the pool to one expression with a short interval and watch an eyed cow. */
    private static void eachExpression(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.cameraAt(0, 1.2, 3.5, 0, 1.1, 0);
        String[][] expressions = {
            {"blink", "at least one shot shows the cow's eyes closed or closing"},
            {"cross_eye", "at least one shot shows the cow's pupils turned toward each other"},
            {"side_eye", "at least one shot shows both pupils glancing to one side"},
            {"stare", "at least one shot shows wide, fixed eyes"}
        };
        for (String[] expression : expressions) {
            s.ambient(20, expression[0]);
            s.burst(5, 4, "Pool is " + expression[0] + " only: " + expression[1] + ".");
        }
    }

    /** In-game: shorten the interval and look away long enough for eviction, then look back. */
    private static void eviction(Scene s) {
        s.ambient(30, "blink", "side_eye", "stare");
        String cow = s.mob("minecraft:cow", -2, 0, 0);
        s.eyes(cow);
        s.frame(0, 2, -2, "south", EYE, 0, false, false);
        s.water(1, -1, 4, 1);
        String boat = s.boat("minecraft:oak_boat", 2.5, 0, 90);
        s.useOnEntity(boat, 2.5, 2.5, 2.5, Scene.SLIMY);
        s.camera(0, 1.5, 5, 0, 0);
        s.waitTicks(600);
        s.camera(0, 1.5, 5, 180, 0);
        s.burst(
                3,
                1,
                "Just after turning back from a long look away: the cow, framed eye, and boat show a plausible current state, not a fast replay through several expressions.");
    }

    /**
     * In-game: two eyed boats, a datapack-paired one and a one-eye one, with a cross-eye-only pool.
     */
    private static void boatCrossing(Scene s) {
        s.datapack(TestPacks.BOAT_PAIRED);
        s.datapack(TestPacks.BOAT_ONE_EYE);
        s.water(-6, -2, 6, 2);
        String bundled = s.boat("minecraft:spruce_boat", -4, 0, 90);
        String paired = s.boat("minecraft:oak_boat", 0, 0, 90);
        String single = s.boat("minecraft:birch_boat", 4, 0, 90);
        s.useOnEntity(bundled, -4, 2.5, 2.5, Scene.SLIMY);
        s.useOnEntity(paired, 0, 2.5, 2.5, Scene.SLIMY);
        s.useOnEntity(single, 4, 2.5, 2.5, Scene.SLIMY);
        s.ambient(20, "cross_eye");
        s.cameraAt(0, 1.2, 7, 0, 0, 0);
        s.burst(
                6,
                5,
                "Cross-eye-only pool. Left spruce boat (bundled eyes) never crosses; middle oak boat (paired by the test datapack) crosses in at least one shot; right birch boat (one eye) never crosses.");
    }

    /** In-game: a cross-eye-only pool with loose eyes in a frame, the hotbar, and both hands. */
    private static void looseNeverCross(Scene s) {
        s.ambient(20, "cross_eye");
        s.frame(1, 1, -2, "south", EYE, 0, false, false);
        s.clearInventory();
        s.give(EYE_ITEM, 3);
        s.hotbar(0);
        s.slot("weapon.offhand", EYE_ITEM);
        s.hud(true);
        s.camera(0, 0, 1, 180, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.burst(
                6,
                10,
                "Cross-eye-only pool: the framed eye, the hotbar eye, and both held eyes never cross.");
    }

    /** In-game: pause with the world mid-expression; world eyes freeze. */
    private static void pause(Scene s) {
        s.ambient(20, "blink", "side_eye");
        String cow = s.mob("minecraft:cow", -2, 0, 0);
        s.eyes(cow);
        s.frame(0, 2, -2, "south", EYE, 0, false, false);
        s.water(1, -1, 4, 1);
        String boat = s.boat("minecraft:oak_boat", 2.5, 0, 90);
        s.useOnEntity(boat, 2.5, 2.5, 2.5, Scene.SLIMY);
        s.clearInventory();
        s.give(EYE_ITEM);
        s.hotbar(0);
        s.camera(0, 0, 5, 180, 10);
        s.view(CameraType.THIRD_PERSON_BACK);
        s.waitTicks(25);
        s.context.setScreen(() -> new PauseScreen(true));
        s.waitTicks(2);
        s.shot(
                "Paused, behind the pause menu: the cow, framed eye, boat, and held eye in some state.");
        s.waitTicks(40);
        s.shot("Still paused, 40 ticks later: every eye looks exactly as in the previous shot.");
    }

    /** In-game: eyed heads with a paired layout, first blink-only, then cross-eye-only. */
    private static void heads(Scene s) {
        s.eyedHead("minecraft:zombie_head[rotation=0]", -1, 0, 0);
        s.eyedHead("minecraft:creeper_head[rotation=0]", 1, 0, 0);
        s.cameraAt(0, 1, 2.5, 0, 0.3, 0);
        s.ambient(20, "blink");
        s.burst(5, 4, "Blink-only pool: when a head blinks, both of its eyes close together.");
        s.ambient(20, "cross_eye");
        s.burst(
                5,
                4,
                "Cross-eye-only pool: at least one shot shows a head's paired eyes turned toward each other.");
    }

    /**
     * In-game: an interval shorter than the longest expression, with all expressions in the pool.
     */
    private static void shortInterval(Scene s) {
        s.ambient(5, "blink", "cross_eye", "side_eye", "stare");
        String cow = s.mob("minecraft:cow", -1.5, 0, 0);
        s.eyes(cow);
        s.frame(1, 1, -2, "south", EYE, 0, false, false);
        s.cameraAt(0, 1.3, 3.5, 0, 1, 0);
        s.burst(
                8,
                3,
                "Interval shorter than the longest expression: each shot shows at most one expression per eye group, and eyes look normal between expressions, never stuck half-closed.");
    }

    /**
     * In-game: identical eyes in four frames, four hotbar slots, both hands, and two boats, random
     * interval.
     */
    private static void independence(Scene s) {
        s.config(
                "ambientBehaviors",
                "true",
                "ambientMinTicks",
                "10",
                "ambientMaxTicks",
                "60",
                "ambientBehaviorPool",
                Scene.tomlList("blink"));
        for (int i = 0; i < 4; i++) {
            s.frame(-3 + i * 2, 2, -3, "south", EYE, 0, false, false);
        }
        s.water(-5, 1, 5, 4);
        String left = s.boat("minecraft:oak_boat", -2, 2.5, 90);
        String right = s.boat("minecraft:oak_boat", 2, 2.5, 90);
        s.useOnEntity(left, -2, 2.5, 5, Scene.SLIMY);
        s.useOnEntity(right, 2, 2.5, 5, Scene.SLIMY);
        s.clearInventory();
        for (int i = 0; i < 4; i++) {
            s.slot("hotbar." + i, EYE_ITEM);
        }
        s.slot("weapon.offhand", EYE_ITEM);
        s.hud(true);
        s.camera(0, 0.5, 8, 180, 15);
        s.view(CameraType.THIRD_PERSON_BACK);
        s.burst(
                5,
                7,
                "Identical eyes in four frames, four hotbar slots, both hands, and two boats, blinking at random intervals: in at least one shot the copies are not all in the same state.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("3.1", "Each pool expression on a mob", () -> eachExpression(s));
                    s.scenario("3.2", "Head expressions", () -> heads(s));
                    s.scenario("3.3", "Loose eyes never cross", () -> looseNeverCross(s));
                    s.scenario("3.4", "Copies animate independently", () -> independence(s));
                    s.scenario("3.5", "Boat cross-eye by layout", () -> boatCrossing(s));
                    s.scenario("3.6", "Short interval, no overlap", () -> shortInterval(s));
                    s.scenario("3.7", "Eviction does not replay", () -> eviction(s));
                    s.scenario("3.8", "Pause freezes world eyes", () -> pause(s));
                });
    }
}
