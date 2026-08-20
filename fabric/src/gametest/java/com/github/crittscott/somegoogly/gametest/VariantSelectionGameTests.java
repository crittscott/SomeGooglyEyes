package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link VariantSelectionGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class VariantSelectionGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

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
