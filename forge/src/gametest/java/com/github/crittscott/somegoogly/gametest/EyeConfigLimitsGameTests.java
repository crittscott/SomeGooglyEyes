package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link EyeConfigLimitsGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeConfigLimitsGameTests {

    private EyeConfigLimitsGameTests() {
    }

    public static void crossTargetMustReferenceAnotherEyeInTheSameHead(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.crossTargetMustReferenceAnotherEyeInTheSameHead(helper);
    }

    public static void variantWeightMustBeNonnegative(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.variantWeightMustBeNonnegative(helper);
    }

    public static void numericPlacementBoundsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.numericPlacementBoundsAreEnforced(helper);
    }

    public static void containerCountCapsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.containerCountCapsAreEnforced(helper);
    }
}
