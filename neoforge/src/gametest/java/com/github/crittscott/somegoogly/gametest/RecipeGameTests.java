package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NeoForge GameTest entry points for {@link RecipeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class RecipeGameTests {

    private RecipeGameTests() {
    }

    public static void cobwebClearsAllOverrides(GameTestHelper helper) {
        RecipeGameTestsLogic.cobwebClearsAllOverrides(helper);
    }

    public static void dyeSetsIrisAndKeepsUnrelatedComponent(GameTestHelper helper) {
        RecipeGameTestsLogic.dyeSetsIrisAndKeepsUnrelatedComponent(helper);
    }

    public static void glowstoneAndRedstoneToggleGlow(GameTestHelper helper) {
        RecipeGameTestsLogic.glowstoneAndRedstoneToggleGlow(helper);
    }

    public static void twoEyesDoNotMatch(GameTestHelper helper) {
        RecipeGameTestsLogic.twoEyesDoNotMatch(helper);
    }

    public static void slimyEyeCarriesTheEyesAppearance(GameTestHelper helper) {
        RecipeGameTestsLogic.slimyEyeCarriesTheEyesAppearance(helper);
    }
}
