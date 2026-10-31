package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link RecipeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class RecipeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void cobwebClearsAllOverrides(GameTestHelper helper) {
        RecipeGameTestsLogic.cobwebClearsAllOverrides(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void dyeSetsIrisAndKeepsUnrelatedComponent(GameTestHelper helper) {
        RecipeGameTestsLogic.dyeSetsIrisAndKeepsUnrelatedComponent(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void glowstoneAndRedstoneToggleGlow(GameTestHelper helper) {
        RecipeGameTestsLogic.glowstoneAndRedstoneToggleGlow(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void twoEyesDoNotMatch(GameTestHelper helper) {
        RecipeGameTestsLogic.twoEyesDoNotMatch(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void slimyEyeCarriesTheEyesAppearance(GameTestHelper helper) {
        RecipeGameTestsLogic.slimyEyeCarriesTheEyesAppearance(helper);
    }
}
