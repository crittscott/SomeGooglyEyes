package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link EyeItemServiceGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class EyeItemServiceGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private EyeItemServiceGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractHarvestsEyesForOneDurability(
                helper, helper.makeMockPlayer(GameType.SURVIVAL));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractPassesWhenNotApplicable(
                helper, helper.makeMockPlayer(GameType.SURVIVAL));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.selfRemoveWithShearsDropsAnEyeAndCostsDurability(
                helper, helper.makeMockPlayer(GameType.SURVIVAL));
    }
}
