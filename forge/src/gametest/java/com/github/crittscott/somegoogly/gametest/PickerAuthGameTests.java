package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class PickerAuthGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private PickerAuthGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerRequestsRequireCreativeAndThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsRequireCreativeAndThrottlePerTick(
                helper, helper.makeMockServerPlayerInLevel());
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }
}
