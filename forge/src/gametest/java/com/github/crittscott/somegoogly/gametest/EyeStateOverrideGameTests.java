package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class EyeStateOverrideGameTests {

    private EyeStateOverrideGameTests() {
    }

    public static void adminAppearanceCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminAppearanceCommands(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminCommandsRefuseUnauthorizedOrInvalidUse(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void adminEyesAndBehaviorCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminEyesAndBehaviorCommands(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }
}
