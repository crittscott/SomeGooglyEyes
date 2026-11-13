package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link HeldEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class HeldEyeGameTests {

    private HeldEyeGameTests() {
    }

    public static void heldEyeBulgesWhenHurt(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeBulgesWhenHurt(helper, helper.makeMockServerPlayerInLevel());
    }

    public static void heldEyeHonorsServerSettings(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeHonorsServerSettings(helper, helper.makeMockServerPlayerInLevel());
    }

    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyePlaysIdleBehaviors(helper, helper.makeMockServerPlayerInLevel());
    }

    public static void heldEyeResetsWhenPutAway(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeResetsWhenPutAway(helper, helper.makeMockServerPlayerInLevel());
    }

    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeSwirlsWhenHealed(helper, helper.makeMockServerPlayerInLevel());
    }

    public static void heldEyeWinksAlone(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeWinksAlone(helper, helper.makeMockServerPlayerInLevel());
    }
}
