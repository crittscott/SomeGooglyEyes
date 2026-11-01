package com.github.crittscott.somegoogly.gametest;


import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link HandItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class HandItemEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handEyesFollowWorldGravityAcrossViews(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesFollowWorldGravityAcrossViews(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handEyesReactToHolderMotion(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesReactToHolderMotion(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handEyesReplaceContentsAndHonorGates(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesReplaceContentsAndHonorGates(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handEyesRetireUnloadedAndHiddenHolders(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesRetireUnloadedAndHiddenHolders(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handEyesSeparateHoldersAndHands(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesSeparateHoldersAndHands(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handEyesUseWorldClockAndPause(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesUseWorldClockAndPause(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void handHeadUsesConfiguredLayout(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handHeadUsesConfiguredLayout(helper);
    }
}
