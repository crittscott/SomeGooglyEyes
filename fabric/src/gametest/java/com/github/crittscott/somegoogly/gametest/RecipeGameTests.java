package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link RecipeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class RecipeGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void cobwebClearsAllOverrides(GameTestHelper helper) {
        RecipeGameTestsLogic.cobwebClearsAllOverrides(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void dyeSetsIrisAndKeepsUnrelatedComponent(GameTestHelper helper) {
        RecipeGameTestsLogic.dyeSetsIrisAndKeepsUnrelatedComponent(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void glowstoneAndRedstoneToggleGlow(GameTestHelper helper) {
        RecipeGameTestsLogic.glowstoneAndRedstoneToggleGlow(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void twoEyesDoNotMatch(GameTestHelper helper) {
        RecipeGameTestsLogic.twoEyesDoNotMatch(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void slimyEyeCarriesTheEyesAppearance(GameTestHelper helper) {
        RecipeGameTestsLogic.slimyEyeCarriesTheEyesAppearance(helper);
    }
}
