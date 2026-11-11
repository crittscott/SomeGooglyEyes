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
        HeldEyeGameTestsLogic.heldEyeBulgesWhenHurt(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void heldEyeHonorsServerSettings(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeHonorsServerSettings(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyePlaysIdleBehaviors(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void heldEyeResetsWhenPutAway(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeResetsWhenPutAway(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeSwirlsWhenHealed(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void heldEyeWinksAlone(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeWinksAlone(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }
}
