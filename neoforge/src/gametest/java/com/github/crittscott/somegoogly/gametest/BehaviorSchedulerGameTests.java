package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link BehaviorSchedulerGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BehaviorSchedulerGameTests {

    private static final String TEMPLATE = "empty";

    private BehaviorSchedulerGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void affectionEventsPlayLove(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.affectionEventsPlayLove(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aimingArcherWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingArcherWinks(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aimingPlayerWinks(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.aimingPlayerWinks(helper);
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
    public static void creepyEndermanPulses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.creepyEndermanPulses(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyelessTrackedMobIgnoresGameEvents(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.eyelessTrackedMobIgnoresGameEvents(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gameEventTriggersRespectConfig(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.gameEventTriggersRespectConfig(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void healSwirlIsRateLimited(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.healSwirlIsRateLimited(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void milkingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.milkingPlaysGrow(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.oneBehaviorAtATimeUntilItElapses(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rammingGoatSquints(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.rammingGoatSquints(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void serverOnlyAngerIsShared(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.serverOnlyAngerIsShared(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void shearingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.shearingPlaysGrow(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sleepingMobPlaysNoBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingMobPlaysNoBehaviors(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sleepingVillagerClosesLids(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingVillagerClosesLids(helper);
    }
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void swellingCreeperRattles(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.swellingCreeperRattles(helper);
    }

}
