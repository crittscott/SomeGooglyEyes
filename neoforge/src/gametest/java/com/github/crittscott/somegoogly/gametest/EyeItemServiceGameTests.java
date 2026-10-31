package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * NeoForge GameTest entry points for {@link EyeItemServiceGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeItemServiceGameTests {

    private EyeItemServiceGameTests() {
    }

    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractHarvestsEyesForOneDurability(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractPassesWhenNotApplicable(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.selfRemoveWithShearsDropsAnEyeAndCostsDurability(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
