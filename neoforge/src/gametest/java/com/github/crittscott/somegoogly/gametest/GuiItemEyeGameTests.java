package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link GuiItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuiItemEyeGameTests {

    private static final String TEMPLATE = "empty";

    private GuiItemEyeGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void guiEyesAdvanceWithoutWorldTicks(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesAdvanceWithoutWorldTicks(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void guiEyesHonorEligibilityAndSwitch(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesHonorEligibilityAndSwitch(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void guiEyesKeepPresentationIdentity(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesKeepPresentationIdentity(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void guiEyesReplaceChangedContents(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesReplaceChangedContents(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void guiEyesRetireClosedAndHiddenViews(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesRetireClosedAndHiddenViews(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void guiEyesSeparateSlotsAndCursor(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiEyesSeparateSlotsAndCursor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void guiHeadUsesConfiguredLayout(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.guiHeadUsesConfiguredLayout(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void nestedDrawContextsRestoreOuterSelection(GameTestHelper helper) {
        GuiItemEyeGameTestsLogic.nestedDrawContextsRestoreOuterSelection(helper);
    }
}
