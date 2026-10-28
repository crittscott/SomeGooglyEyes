package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link BehaviorSchedulerGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BehaviorSchedulerGameTests {

    private static final String TEMPLATE = "empty";

    private BehaviorSchedulerGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.oneBehaviorAtATimeUntilItElapses(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void healSwirlIsRateLimited(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.healSwirlIsRateLimited(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gameEventTriggersRespectConfig(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.gameEventTriggersRespectConfig(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyelessTrackedMobIgnoresGameEvents(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.eyelessTrackedMobIgnoresGameEvents(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void affectionEventsPlayLove(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.affectionEventsPlayLove(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void milkingPlaysGrow(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.milkingPlaysGrow(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sleepingMobPlaysNoBehaviors(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.sleepingMobPlaysNoBehaviors(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
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
    public static void teleportPlaysJolt(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.teleportPlaysJolt(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void joltFlingsPupils(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.joltFlingsPupils(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rammingGoatSquints(GameTestHelper helper) {
        BehaviorSchedulerGameTestsLogic.rammingGoatSquints(helper);
    }
}
