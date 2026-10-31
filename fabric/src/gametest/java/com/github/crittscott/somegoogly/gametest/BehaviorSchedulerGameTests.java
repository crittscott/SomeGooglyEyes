package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link BehaviorSchedulerGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class BehaviorSchedulerGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void affectionEventsPlayLove(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.affectionEventsPlayLove(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void aimingArcherWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingArcherWinks(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void aimingPlayerWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingPlayerWinks(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void angerFollowsEntityLifetime(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.angerFollowsEntityLifetime(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void angryMobsHaveRedPupils(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.angryMobsHaveRedPupils(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void creepyEndermanPulses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.creepyEndermanPulses(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyelessTrackedMobIgnoresGameEvents(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.eyelessTrackedMobIgnoresGameEvents(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void gameEventTriggersRespectConfig(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.gameEventTriggersRespectConfig(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void healSwirlIsRateLimited(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.healSwirlIsRateLimited(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void milkingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.milkingPlaysGrow(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.oneBehaviorAtATimeUntilItElapses(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void rammingGoatSquints(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.rammingGoatSquints(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void serverOnlyAngerIsShared(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.serverOnlyAngerIsShared(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void shearingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.shearingPlaysGrow(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void sleepingMobPlaysNoBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingMobPlaysNoBehaviors(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void sleepingVillagerClosesLids(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingVillagerClosesLids(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void swellingCreeperRattles(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.swellingCreeperRattles(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void switchedOffOngoingBehaviorsStayNeutral(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.switchedOffOngoingBehaviorsStayNeutral(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void receivedExpressionsOutliveRenderTrackers(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.receivedExpressionsOutliveRenderTrackers(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void disabledEyesSuspendAutomaticBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.disabledEyesSuspendAutomaticBehaviors(helper, FakePlayer.get(helper.getLevel()));
    }
}
