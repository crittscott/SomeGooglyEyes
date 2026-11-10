package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EyeStateOverrideGameTests {

    private static final String TEMPLATE = "empty";

    private EyeStateOverrideGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void adminAppearanceCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminAppearanceCommands(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminCommandsRefuseUnauthorizedOrInvalidUse(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void adminEyesAndBehaviorCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminEyesAndBehaviorCommands(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}

