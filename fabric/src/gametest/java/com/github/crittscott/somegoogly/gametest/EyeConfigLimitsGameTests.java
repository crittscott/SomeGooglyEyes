package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EyeConfigLimitsGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeConfigLimitsGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crossTargetMustReferenceAnotherEyeInTheSameHead(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.crossTargetMustReferenceAnotherEyeInTheSameHead(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void variantWeightMustBeNonnegative(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.variantWeightMustBeNonnegative(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void numericPlacementBoundsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.numericPlacementBoundsAreEnforced(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void containerCountCapsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.containerCountCapsAreEnforced(helper);
    }
}
