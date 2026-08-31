package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link VariantSelectionGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class VariantSelectionGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private VariantSelectionGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void cumulativeWeightBoundariesPickExpectedVariant(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.cumulativeWeightBoundariesPickExpectedVariant(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void degenerateConfigsFallToFirstVariant(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.degenerateConfigsFallToFirstVariant(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rollIsDeterministicForAConfig(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.rollIsDeterministicForAConfig(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void weightDefaultsAndClamping(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.weightDefaultsAndClamping(helper);
    }
}
