package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerAuthGameTests {

    private static final String TEMPLATE = "empty";

    private PickerAuthGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerRequestsRequireCreativeAndThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsRequireCreativeAndThrottlePerTick(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }
}
