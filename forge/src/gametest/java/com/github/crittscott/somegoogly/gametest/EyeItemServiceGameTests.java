package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link EyeItemServiceGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeItemServiceGameTests {

    private EyeItemServiceGameTests() {
    }

    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractHarvestsEyesForOneDurability(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractPassesWhenNotApplicable(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.selfRemoveWithShearsDropsAnEyeAndCostsDurability(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }
}
