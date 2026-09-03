package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link EyeConfigLimitsGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class EyeConfigLimitsGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private EyeConfigLimitsGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crossTargetMustReferenceAnotherEyeInTheSameHead(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.crossTargetMustReferenceAnotherEyeInTheSameHead(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void numericPlacementBoundsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.numericPlacementBoundsAreEnforced(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void colorChannelsMustBeInRange(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.colorChannelsMustBeInRange(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void containerCountCapsAreEnforced(GameTestHelper helper) {
        EyeConfigLimitsGameTestsLogic.containerCountCapsAreEnforced(helper);
    }
}
