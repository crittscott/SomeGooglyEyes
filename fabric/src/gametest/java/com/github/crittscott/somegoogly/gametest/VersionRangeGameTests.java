package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link VersionRangeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class VersionRangeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void exactVersionMatchesOnlyItself(GameTestHelper helper) {
        VersionRangeGameTestsLogic.exactVersionMatchesOnlyItself(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void malformedRangeDoesNotMatch(GameTestHelper helper) {
        VersionRangeGameTestsLogic.malformedRangeDoesNotMatch(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void rangeBoundsRespectInclusivity(GameTestHelper helper) {
        VersionRangeGameTestsLogic.rangeBoundsRespectInclusivity(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void nearestPicksNewestOlderGeneration(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestPicksNewestOlderGeneration(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void nearestPicksOldestNewerGenerationOnDowngrade(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestPicksOldestNewerGenerationOnDowngrade(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void nearestGapResolvesToOlderNeighbor(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestGapResolvesToOlderNeighbor(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void nearestHandlesExactAndMalformedDeclarations(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestHandlesExactAndMalformedDeclarations(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void entirelyBelowSplitsStaleFromDowngrade(GameTestHelper helper) {
        VersionRangeGameTestsLogic.entirelyBelowSplitsStaleFromDowngrade(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void shorterVersionPadsWithZero(GameTestHelper helper) {
        VersionRangeGameTestsLogic.shorterVersionPadsWithZero(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void optionalModVersionRangeSynthesis(GameTestHelper helper) {
        VersionRangeGameTestsLogic.optionalModVersionRangeSynthesis(helper);
    }
}
