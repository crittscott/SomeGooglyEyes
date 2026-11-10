package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link HeldEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeldEyeGameTests {

    private static final String TEMPLATE = "empty";

    private HeldEyeGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeBulgesWhenHurt(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeBulgesWhenHurt(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeHonorsServerSettings(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeHonorsServerSettings(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyePlaysIdleBehaviors(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeResetsWhenPutAway(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeResetsWhenPutAway(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "somegoogly_held_heal")
    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeSwirlsWhenHealed(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void heldEyeWinksAlone(GameTestHelper helper) {
        HeldEyeGameTestsLogic.heldEyeWinksAlone(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
