package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EligibilityGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EligibilityGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void usablePredicateEndpoints(GameTestHelper helper) {
        EligibilityGameTestsLogic.usablePredicateEndpoints(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void eligibilityFollowsTheSharedPredicate(GameTestHelper helper) {
        EligibilityGameTestsLogic.eligibilityFollowsTheSharedPredicate(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeAppliesOnlyToEligibleTargets(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeAppliesOnlyToEligibleTargets(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeRefusesAnAlreadyEyedTarget(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRefusesAnAlreadyEyedTarget(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeRerollsThePlacementVariant(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRerollsThePlacementVariant(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void canEverWearEyesSpansBothAgeBuckets(GameTestHelper helper) {
        EligibilityGameTestsLogic.canEverWearEyesSpansBothAgeBuckets(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void naturalDecisionRespectsMasterToggleAndIsOneShot(GameTestHelper helper) {
        EligibilityGameTestsLogic.naturalDecisionRespectsMasterToggleAndIsOneShot(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeCreativeApplicationDoesNotConsume(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeCreativeApplicationDoesNotConsume(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeSelfApplyRequiresSneak(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeSelfApplyRequiresSneak(
                helper, FakePlayer.get(helper.getLevel()));
    }
}
