package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link GuiItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class GuiItemEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiEyesAdvanceWithoutWorldTicks(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesAdvanceWithoutWorldTicks(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiEyesHonorEligibilityAndSwitch(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesHonorEligibilityAndSwitch(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiEyesKeepPresentationIdentity(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesKeepPresentationIdentity(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiEyesReplaceChangedContents(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesReplaceChangedContents(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiEyesRetireClosedAndHiddenViews(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesRetireClosedAndHiddenViews(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiEyesSeparateSlotsAndCursor(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesSeparateSlotsAndCursor(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void guiHeadUsesConfiguredLayout(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiHeadUsesConfiguredLayout(helper);
    }
}
