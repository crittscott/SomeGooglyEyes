package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * NeoForge GameTest entry points for {@link BehaviorSchedulerGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class BehaviorSchedulerGameTests {

    private BehaviorSchedulerGameTests() {
    }

    public static void affectionEventsPlayLove(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.affectionEventsPlayLove(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void aimingArcherWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingArcherWinks(helper);
    }

    public static void aimingPlayerWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingPlayerWinks(helper);
    }

    public static void angerFollowsEntityLifetime(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.angerFollowsEntityLifetime(helper);
    }

    public static void angryMobsHaveRedPupils(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.angryMobsHaveRedPupils(helper);
    }

    public static void creepyEndermanPulses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.creepyEndermanPulses(helper);
    }

    public static void eyelessTrackedMobIgnoresGameEvents(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.eyelessTrackedMobIgnoresGameEvents(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void gameEventTriggersRespectConfig(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.gameEventTriggersRespectConfig(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void healSwirlIsRateLimited(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.healSwirlIsRateLimited(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void milkingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.milkingPlaysGrow(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.oneBehaviorAtATimeUntilItElapses(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void rammingGoatSquints(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.rammingGoatSquints(helper);
    }

    public static void serverOnlyAngerIsShared(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.serverOnlyAngerIsShared(helper);
    }

    public static void shearingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.shearingPlaysGrow(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void sleepingMobPlaysNoBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingMobPlaysNoBehaviors(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void sleepingVillagerClosesLids(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingVillagerClosesLids(helper);
    }

    public static void swellingCreeperRattles(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.swellingCreeperRattles(helper);
    }

    public static void switchedOffOngoingBehaviorsStayNeutral(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.switchedOffOngoingBehaviorsStayNeutral(helper);
    }

    public static void receivedExpressionsOutliveRenderTrackers(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.receivedExpressionsOutliveRenderTrackers(helper);
    }

    public static void disabledEyesSuspendAutomaticBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.disabledEyesSuspendAutomaticBehaviors(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
