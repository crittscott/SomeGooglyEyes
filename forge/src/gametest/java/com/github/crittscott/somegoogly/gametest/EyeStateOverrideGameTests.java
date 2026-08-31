package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class EyeStateOverrideGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void clearingTintsPreservesGlow(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingTintsPreservesGlow(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void snapshotAppliesAllFieldsTogether(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.snapshotAppliesAllFieldsTogether(helper);
    }
}
