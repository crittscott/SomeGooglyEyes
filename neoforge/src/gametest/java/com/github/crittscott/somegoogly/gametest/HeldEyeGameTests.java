package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * NeoForge GameTest entry points for {@link HeldEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class HeldEyeGameTests {

    private HeldEyeGameTests() {
    }

    public static void heldEyeBulgesWhenHurt(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeBulgesWhenHurt(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void heldEyeHonorsServerSettings(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeHonorsServerSettings(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyePlaysIdleBehaviors(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void heldEyeResetsWhenPutAway(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeResetsWhenPutAway(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeSwirlsWhenHealed(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void heldEyeWinksAlone(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeWinksAlone(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
