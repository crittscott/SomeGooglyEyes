package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * NeoForge GameTest entry points for {@link EligibilityGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EligibilityGameTests {

    private EligibilityGameTests() {
    }

    public static void usablePredicateEndpoints(GameTestHelper helper) {
        EligibilityGameTestsLogic.usablePredicateEndpoints(helper);
    }

    public static void eligibilityFollowsTheSharedPredicate(GameTestHelper helper) {
        EligibilityGameTestsLogic.eligibilityFollowsTheSharedPredicate(helper);
    }

    public static void slimyEyeAppliesOnlyToEligibleTargets(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeAppliesOnlyToEligibleTargets(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void slimyEyeRefusesAnAlreadyEyedTarget(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRefusesAnAlreadyEyedTarget(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void slimyEyeRerollsThePlacementVariant(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRerollsThePlacementVariant(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void canEverWearEyesSpansBothAgeBuckets(GameTestHelper helper) {
        EligibilityGameTestsLogic.canEverWearEyesSpansBothAgeBuckets(helper);
    }

    public static void naturalDecisionRespectsMasterToggleAndIsOneShot(GameTestHelper helper) {
        EligibilityGameTestsLogic.naturalDecisionRespectsMasterToggleAndIsOneShot(helper);
    }

    public static void slimyEyeCreativeApplicationDoesNotConsume(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeCreativeApplicationDoesNotConsume(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void slimyEyeSelfApplyRequiresSneak(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeSelfApplyRequiresSneak(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
