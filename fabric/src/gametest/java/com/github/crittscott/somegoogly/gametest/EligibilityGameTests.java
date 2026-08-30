package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EligibilityGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EligibilityGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void usablePredicateEndpoints(GameTestHelper helper) {
        EligibilityGameTestsLogic.usablePredicateEndpoints(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void eligibilityFollowsTheSharedPredicate(GameTestHelper helper) {
        EligibilityGameTestsLogic.eligibilityFollowsTheSharedPredicate(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void slimyEyeAppliesOnlyToEligibleTargets(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeAppliesOnlyToEligibleTargets(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void slimyEyeRefusesAnAlreadyEyedTarget(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRefusesAnAlreadyEyedTarget(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void slimyEyeRerollsThePlacementVariant(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRerollsThePlacementVariant(
                helper, FakePlayer.get(helper.getLevel()));
    }
}
