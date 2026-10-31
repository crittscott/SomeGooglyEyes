package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link EyeStateOverrideGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class EyeStateOverrideGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void clearingTintRemovesOnlyThatField(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingTintRemovesOnlyThatField(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void glowOverrideCanBeSetAndDropped(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.glowOverrideCanBeSetAndDropped(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void settingOneFieldLeavesOthersAbsent(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.settingOneFieldLeavesOthersAbsent(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void clearingEveryFieldRemovesTheCompound(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingEveryFieldRemovesTheCompound(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void clearingTintsPreservesGlow(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.clearingTintsPreservesGlow(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void snapshotAppliesAllFieldsTogether(GameTestHelper helper) {
        EyeStateOverrideGameTestsLogic.snapshotAppliesAllFieldsTogether(helper);
    }
}
