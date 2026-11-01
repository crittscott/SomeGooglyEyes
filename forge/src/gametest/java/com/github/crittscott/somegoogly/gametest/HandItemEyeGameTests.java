package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link HandItemEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class HandItemEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private HandItemEyeGameTests() {
    }

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
