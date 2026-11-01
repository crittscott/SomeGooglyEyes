package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link FramedEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class FramedEyeGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE)
    public void framedEyeFiltersItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFiltersItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyeFollowsMountedGravity(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFollowsMountedGravity(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyeHandlesShortIntervalsAndTimeJumps(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHandlesShortIntervalsAndTimeJumps(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyeHonorsAmbientSettings(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHonorsAmbientSettings(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyePlaysSingleEyeExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyePlaysSingleEyeExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyeReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReconstructsExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyeReleasesExpressionOverlays(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReleasesExpressionOverlays(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedEyeUsesIndependentSchedules(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeUsesIndependentSchedules(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedHeadAllowsPairedExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadAllowsPairedExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedHeadInvalidatesChangedItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadInvalidatesChangedItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedHeadReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadReconstructsExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void framedHeadUsesConfiguredLayout(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadUsesConfiguredLayout(helper);
    }
}
