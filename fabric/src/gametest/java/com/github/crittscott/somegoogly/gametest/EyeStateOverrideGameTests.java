package com.github.crittscott.somegoogly.gametest;

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
