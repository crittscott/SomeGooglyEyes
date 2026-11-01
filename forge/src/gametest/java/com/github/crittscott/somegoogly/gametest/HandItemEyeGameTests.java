package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link HandItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class HandItemEyeGameTests {


    private HandItemEyeGameTests() {
    }

    public static void handEyesFollowWorldGravityAcrossViews(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesFollowWorldGravityAcrossViews(helper);
    }

    public static void handEyesReactToHolderMotion(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesReactToHolderMotion(helper);
    }

    public static void handEyesReplaceContentsAndHonorGates(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesReplaceContentsAndHonorGates(helper);
    }

    public static void handEyesRetireUnloadedAndHiddenHolders(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesRetireUnloadedAndHiddenHolders(helper);
    }

    public static void handEyesSeparateHoldersAndHands(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesSeparateHoldersAndHands(helper);
    }

    public static void handEyesUseWorldClockAndPause(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesUseWorldClockAndPause(helper);
    }

    public static void handHeadUsesConfiguredLayout(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handHeadUsesConfiguredLayout(helper);
    }
}
