package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link VariantSelectionGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class VariantSelectionGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void cumulativeWeightBoundariesPickExpectedVariant(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.cumulativeWeightBoundariesPickExpectedVariant(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void degenerateConfigsFallToFirstVariant(GameTestHelper helper) {
        VariantSelectionGameTestsLogic.degenerateConfigsFallToFirstVariant(helper);
    }
}
