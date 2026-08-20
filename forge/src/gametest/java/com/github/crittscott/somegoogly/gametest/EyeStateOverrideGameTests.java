package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EyeStateOverrideGameTests {

    private static final String TEMPLATE = "empty";

    private EyeStateOverrideGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void clearingTintRemovesOnlyThatField(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingTintRemovesOnlyThatField(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void glowOverrideCanBeSetAndDropped(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.glowOverrideCanBeSetAndDropped(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void settingOneFieldLeavesOthersAbsent(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.settingOneFieldLeavesOthersAbsent(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void clearingEveryFieldRemovesTheCompound(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingEveryFieldRemovesTheCompound(helper);
    }
}
