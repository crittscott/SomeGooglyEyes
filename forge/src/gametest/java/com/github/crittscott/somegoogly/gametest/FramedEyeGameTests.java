package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link FramedEyeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class FramedEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private FramedEyeGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeFiltersItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFiltersItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeFollowsMountedGravity(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeFollowsMountedGravity(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeHandlesShortIntervalsAndTimeJumps(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHandlesShortIntervalsAndTimeJumps(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeHonorsAmbientSettings(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeHonorsAmbientSettings(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyePlaysSingleEyeExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyePlaysSingleEyeExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReconstructsExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeReleasesExpressionOverlays(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeReleasesExpressionOverlays(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedEyeUsesIndependentSchedules(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedEyeUsesIndependentSchedules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedHeadAllowsPairedExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadAllowsPairedExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedHeadInvalidatesChangedItems(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadInvalidatesChangedItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedHeadReconstructsExpressions(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadReconstructsExpressions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void framedHeadUsesConfiguredLayout(GameTestHelper helper) {
        FramedEyeGameTestsLogic.framedHeadUsesConfiguredLayout(helper);
    }
}
