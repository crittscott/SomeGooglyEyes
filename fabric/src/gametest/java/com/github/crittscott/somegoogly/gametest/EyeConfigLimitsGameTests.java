package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EyeConfigLimitsGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeConfigLimitsGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void crossTargetMustReferenceAnotherEyeInTheSameHead(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.crossTargetMustReferenceAnotherEyeInTheSameHead(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void variantWeightMustBeNonnegative(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.variantWeightMustBeNonnegative(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void numericPlacementBoundsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.numericPlacementBoundsAreEnforced(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void containerCountCapsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.containerCountCapsAreEnforced(helper);
    }
}
