package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link HandItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class HandItemEyeGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE)
    public static void handEyesReplaceContentsAndHonorGates(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesReplaceContentsAndHonorGates(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void handEyesRetireUnloadedAndHiddenHolders(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesRetireUnloadedAndHiddenHolders(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void handEyesSeparateHoldersAndHands(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesSeparateHoldersAndHands(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void handEyesUseWorldClockAndPause(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handEyesUseWorldClockAndPause(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void handHeadUsesConfiguredLayout(GameTestHelper helper) {
        HandItemEyeGameTestsLogic.handHeadUsesConfiguredLayout(helper);
    }
}
