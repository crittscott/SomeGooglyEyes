package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * NeoForge GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class EyeStateOverrideGameTests {

    private EyeStateOverrideGameTests() {
    }

    public static void adminAppearanceCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminAppearanceCommands(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminCommandsRefuseUnauthorizedOrInvalidUse(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void adminEyesAndBehaviorCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminEyesAndBehaviorCommands(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
