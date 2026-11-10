package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link HeldEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class HeldEyeGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE)
    public static void heldEyeBulgesWhenHurt(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeBulgesWhenHurt(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeHonorsServerSettings(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeHonorsServerSettings(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyePlaysIdleBehaviors(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeResetsWhenPutAway(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeResetsWhenPutAway(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "somegoogly_held_heal")
    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeSwirlsWhenHealed(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeWinksAlone(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeWinksAlone(helper, FakePlayer.get(helper.getLevel()));
    }
}
