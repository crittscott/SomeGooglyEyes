package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link VersionRangeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class VersionRangeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private VersionRangeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void exactVersionMatchesOnlyItself(GameTestHelper helper) {
        VersionRangeGameTestsLogic.exactVersionMatchesOnlyItself(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void malformedRangeDoesNotMatch(GameTestHelper helper) {
        VersionRangeGameTestsLogic.malformedRangeDoesNotMatch(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rangeBoundsRespectInclusivity(GameTestHelper helper) {
        VersionRangeGameTestsLogic.rangeBoundsRespectInclusivity(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestPicksNewestOlderGeneration(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestPicksNewestOlderGeneration(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestPicksOldestNewerGenerationOnDowngrade(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestPicksOldestNewerGenerationOnDowngrade(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestGapResolvesToOlderNeighbor(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestGapResolvesToOlderNeighbor(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestHandlesExactAndMalformedDeclarations(GameTestHelper helper) {
        VersionRangeGameTestsLogic.nearestHandlesExactAndMalformedDeclarations(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void entirelyBelowSplitsStaleFromDowngrade(GameTestHelper helper) {
        VersionRangeGameTestsLogic.entirelyBelowSplitsStaleFromDowngrade(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void shorterVersionPadsWithZero(GameTestHelper helper) {
        VersionRangeGameTestsLogic.shorterVersionPadsWithZero(helper);
    }
}
