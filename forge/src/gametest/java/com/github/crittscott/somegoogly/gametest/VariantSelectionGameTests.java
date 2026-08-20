package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link VariantSelectionGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VariantSelectionGameTests {

    private static final String TEMPLATE = "empty";

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
