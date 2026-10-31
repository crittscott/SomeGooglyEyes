package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NeoForge GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class EyeStateOverrideGameTests {

    private EyeStateOverrideGameTests() {
    }

    public static void clearingTintRemovesOnlyThatField(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingTintRemovesOnlyThatField(helper);
    }

    public static void glowOverrideCanBeSetAndDropped(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.glowOverrideCanBeSetAndDropped(helper);
    }

    public static void settingOneFieldLeavesOthersAbsent(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.settingOneFieldLeavesOthersAbsent(helper);
    }

    public static void clearingEveryFieldRemovesTheCompound(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingEveryFieldRemovesTheCompound(helper);
    }

    public static void clearingTintsPreservesGlow(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingTintsPreservesGlow(helper);
    }

    public static void snapshotAppliesAllFieldsTogether(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.snapshotAppliesAllFieldsTogether(helper);
    }
}

