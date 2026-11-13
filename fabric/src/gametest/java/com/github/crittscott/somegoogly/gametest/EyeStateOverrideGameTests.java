package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.fabricmc.fabric.api.entity.FakePlayer;

/**
 * Fabric GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class EyeStateOverrideGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void adminAppearanceCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminAppearanceCommands(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminCommandsRefuseUnauthorizedOrInvalidUse(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void adminEyesAndBehaviorCommands(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.adminEyesAndBehaviorCommands(helper, FakePlayer.get(helper.getLevel()));
    }
}
