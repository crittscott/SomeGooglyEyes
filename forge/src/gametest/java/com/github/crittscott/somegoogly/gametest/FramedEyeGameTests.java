package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link FramedEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class FramedEyeGameTests {

    private FramedEyeGameTests() {
    }

    public static void framedEyeFiltersItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFiltersItems(helper);
    }

    public static void framedEyeFollowsMountedGravity(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFollowsMountedGravity(helper);
    }

    public static void framedEyeHandlesShortIntervalsAndTimeJumps(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHandlesShortIntervalsAndTimeJumps(helper);
    }

    public static void framedEyeHonorsAmbientSettings(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHonorsAmbientSettings(helper);
    }

    public static void framedEyePlaysSingleEyeExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyePlaysSingleEyeExpressions(helper);
    }

    public static void framedEyeReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReconstructsExpressions(helper);
    }

    public static void framedEyeReleasesExpressionOverlays(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReleasesExpressionOverlays(helper);
    }

    public static void framedEyeUsesIndependentSchedules(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeUsesIndependentSchedules(helper);
    }

    public static void framedHeadAllowsPairedExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadAllowsPairedExpressions(helper);
    }

    public static void framedHeadInvalidatesChangedItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadInvalidatesChangedItems(helper);
    }

    public static void framedHeadReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadReconstructsExpressions(helper);
    }

    public static void framedHeadUsesConfiguredLayout(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadUsesConfiguredLayout(helper);
    }
}
