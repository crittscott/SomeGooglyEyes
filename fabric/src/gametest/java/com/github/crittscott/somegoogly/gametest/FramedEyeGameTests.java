package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link FramedEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class FramedEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeFiltersItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFiltersItems(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeFollowsMountedGravity(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFollowsMountedGravity(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeHandlesShortIntervalsAndTimeJumps(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHandlesShortIntervalsAndTimeJumps(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeHonorsAmbientSettings(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHonorsAmbientSettings(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyePlaysSingleEyeExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyePlaysSingleEyeExpressions(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReconstructsExpressions(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeReleasesExpressionOverlays(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReleasesExpressionOverlays(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedEyeUsesIndependentSchedules(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeUsesIndependentSchedules(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedHeadAllowsPairedExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadAllowsPairedExpressions(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedHeadInvalidatesChangedItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadInvalidatesChangedItems(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedHeadReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadReconstructsExpressions(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void framedHeadUsesConfiguredLayout(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadUsesConfiguredLayout(helper);
    }
}
