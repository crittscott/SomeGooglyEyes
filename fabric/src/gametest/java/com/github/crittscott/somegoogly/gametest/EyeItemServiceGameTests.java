package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EyeItemServiceGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeItemServiceGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractHarvestsEyesForOneDurability(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void optometristInteractPassesWhenNotApplicable(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractPassesWhenNotApplicable(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.selfRemoveWithShearsDropsAnEyeAndCostsDurability(
                helper, FakePlayer.get(helper.getLevel()));
    }
}
