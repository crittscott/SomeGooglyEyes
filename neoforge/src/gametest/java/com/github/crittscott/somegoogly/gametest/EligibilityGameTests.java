package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link EligibilityGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EligibilityGameTests {

    private static final String TEMPLATE = "empty";

    private EligibilityGameTests() {
    }

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
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void slimyEyeRefusesAnAlreadyEyedTarget(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRefusesAnAlreadyEyedTarget(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void slimyEyeRerollsThePlacementVariant(GameTestHelper helper) {
        EligibilityGameTestsLogic.slimyEyeRerollsThePlacementVariant(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
