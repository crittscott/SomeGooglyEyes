package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link VersionRangeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class VersionRangeGameTests {

    private VersionRangeGameTests() {
    }

    public static void exactVersionMatchesOnlyItself(GameTestHelper helper) {
        VersionRangeGameTestsLogic.exactVersionMatchesOnlyItself(helper);
    }

    public static void malformedRangeDoesNotMatch(GameTestHelper helper) {
        VersionRangeGameTestsLogic.malformedRangeDoesNotMatch(helper);
    }

    public static void rangeBoundsRespectInclusivity(GameTestHelper helper) {
        VersionRangeGameTestsLogic.rangeBoundsRespectInclusivity(helper);
    }

    public static void nearestPicksNewestOlderGeneration(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestPicksNewestOlderGeneration(helper);
    }

    public static void nearestPicksOldestNewerGenerationOnDowngrade(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestPicksOldestNewerGenerationOnDowngrade(helper);
    }

    public static void nearestGapResolvesToOlderNeighbor(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestGapResolvesToOlderNeighbor(helper);
    }

    public static void nearestHandlesExactAndMalformedDeclarations(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestHandlesExactAndMalformedDeclarations(helper);
    }

    public static void entirelyBelowSplitsStaleFromDowngrade(GameTestHelper helper) {
        VersionRangeGameTestsLogic.entirelyBelowSplitsStaleFromDowngrade(helper);
    }

    public static void shorterVersionPadsWithZero(GameTestHelper helper) {
        VersionRangeGameTestsLogic.shorterVersionPadsWithZero(helper);
    }

    public static void optionalModVersionRangeSynthesis(GameTestHelper helper) {
        VersionRangeGameTestsLogic.optionalModVersionRangeSynthesis(helper);
    }
}
