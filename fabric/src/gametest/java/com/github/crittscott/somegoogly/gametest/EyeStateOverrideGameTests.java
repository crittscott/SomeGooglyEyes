package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class EyeStateOverrideGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void adminAppearanceCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminAppearanceCommands(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminCommandsRefuseUnauthorizedOrInvalidUse(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void adminEyesAndBehaviorCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminEyesAndBehaviorCommands(helper, FakePlayer.get(helper.getLevel()));
    }
}
