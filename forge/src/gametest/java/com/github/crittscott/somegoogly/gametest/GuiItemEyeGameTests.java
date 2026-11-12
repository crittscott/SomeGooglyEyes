package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link GuiItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class GuiItemEyeGameTests {

    private GuiItemEyeGameTests() {
    }

    public static void guiEyesAdvanceWithoutWorldTicks(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesAdvanceWithoutWorldTicks(helper);
    }

    public static void guiEyesHonorEligibilityAndSwitch(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesHonorEligibilityAndSwitch(helper);
    }

    public static void guiEyesKeepPresentationIdentity(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesKeepPresentationIdentity(helper);
    }

    public static void guiEyesReplaceChangedContents(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesReplaceChangedContents(helper);
    }

    public static void guiEyesRetireClosedAndHiddenViews(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesRetireClosedAndHiddenViews(helper);
    }

    public static void guiEyesSeparateSlotsAndCursor(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesSeparateSlotsAndCursor(helper);
    }

    public static void guiHeadUsesConfiguredLayout(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiHeadUsesConfiguredLayout(helper);
    }

    public static void nestedDrawContextsRestoreOuterSelection(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.nestedDrawContextsRestoreOuterSelection(helper);
    }
}
