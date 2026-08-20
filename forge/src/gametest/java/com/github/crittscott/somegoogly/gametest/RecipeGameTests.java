package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link RecipeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeGameTests {

    private static final String TEMPLATE = "empty";

    private RecipeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void cobwebClearsAllOverrides(GameTestHelper helper) {
        RecipeGameTestsLogic.cobwebClearsAllOverrides(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void dyeSetsIrisAndKeepsUnrelatedNbt(GameTestHelper helper) {
        RecipeGameTestsLogic.dyeSetsIrisAndKeepsUnrelatedNbt(helper);
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
