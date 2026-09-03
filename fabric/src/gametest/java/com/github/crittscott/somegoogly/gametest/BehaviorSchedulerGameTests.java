package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link BehaviorSchedulerGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class BehaviorSchedulerGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.oneBehaviorAtATimeUntilItElapses(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void healSwirlIsRateLimited(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.healSwirlIsRateLimited(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gameEventTriggersRespectConfig(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.gameEventTriggersRespectConfig(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyelessTrackedMobIgnoresGameEvents(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.eyelessTrackedMobIgnoresGameEvents(
                helper, FakePlayer.get(helper.getLevel()));
    }
}
