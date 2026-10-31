package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NeoForge GameTest entry points for {@link VariantSelectionGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class VariantSelectionGameTests {

    private VariantSelectionGameTests() {
    }

    public static void cumulativeWeightBoundariesPickExpectedVariant(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.cumulativeWeightBoundariesPickExpectedVariant(helper);
    }

    public static void degenerateConfigsFallToFirstVariant(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.degenerateConfigsFallToFirstVariant(helper);
    }
}
