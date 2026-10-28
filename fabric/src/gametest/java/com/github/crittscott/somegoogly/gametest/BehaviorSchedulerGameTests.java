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

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void affectionEventsPlayLove(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.affectionEventsPlayLove(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void milkingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.milkingPlaysGrow(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void shearingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.shearingPlaysGrow(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sleepingMobPlaysNoBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingMobPlaysNoBehaviors(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sleepingMobSquints(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingMobSquints(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void swellingCreeperRattles(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.swellingCreeperRattles(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void angerFollowsEntityLifetime(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.angerFollowsEntityLifetime(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void angryMobsHaveRedPupils(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.angryMobsHaveRedPupils(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void serverOnlyAngerIsShared(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.serverOnlyAngerIsShared(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void creepyEndermanPulses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.creepyEndermanPulses(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rammingGoatSquints(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.rammingGoatSquints(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aimingArcherWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingArcherWinks(helper);
    }
}
