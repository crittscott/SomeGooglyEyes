package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link RecipeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class RecipeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private RecipeGameTests() {
    }

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
