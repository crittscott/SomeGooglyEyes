package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Pure-logic coverage of {@link VersionRangeMatcher}: {@code matches} (bracket-range bounds,
 * exact-version matching, the zero-padding equivalence {@code 1.20} ≡ {@code 1.20.0}, malformed input)
 * and the nearest-generation fallback ({@code nearestVersion} / {@code isEntirelyBelow}) used when no
 * entry matches the installed version. These need no world; they spawn nothing and {@code succeed()}
 * immediately. The matcher gates whether (and which) eye config loads for a namespace, so its bounds
 * behavior is worth pinning.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VersionRangeGameTests {

    private static final String TEMPLATE = "empty";

    private VersionRangeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void exactVersionMatchesOnlyItself(GameTestHelper helper) {
        helper.assertTrue(VersionRangeMatcher.matches("1.20.1", "1.20.1"), "exact version should match itself");
        helper.assertTrue(!VersionRangeMatcher.matches("1.20.1", "1.20.2"), "exact version should not match a different one");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void malformedRangeDoesNotMatch(GameTestHelper helper) {
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1", "1.20.1"), "range with no closing bracket should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1-1.21)", "1.20.5"), "range with no comma should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("", "1.20.1"), "blank range should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1,1.21)", ""), "blank version should not match");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void rangeBoundsRespectInclusivity(GameTestHelper helper) {
        // [lower,upper): lower inclusive, upper exclusive.
        helper.assertTrue(VersionRangeMatcher.matches("[1.20.1,1.21)", "1.20.1"), "inclusive lower bound should match");
        helper.assertTrue(VersionRangeMatcher.matches("[1.20.1,1.21)", "1.20.6"), "value inside range should match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1,1.21)", "1.21"), "exclusive upper bound should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1,1.21)", "1.20.0"), "value below lower bound should not match");

        // (lower,upper]: lower exclusive, upper inclusive.
        helper.assertTrue(!VersionRangeMatcher.matches("(1.20,1.21]", "1.20"), "exclusive lower bound should not match");
        helper.assertTrue(VersionRangeMatcher.matches("(1.20,1.21]", "1.21"), "inclusive upper bound should match");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestPicksNewestOlderGeneration(GameTestHelper helper) {
        // Installed version newer than every range: the stale-datapack case.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.1,1.2)"), "1.3");
        helper.assertTrue("[1.1,1.2)".equals(pick),
                "installed above all ranges should pick the newest generation, got " + pick);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestPicksOldestNewerGenerationOnDowngrade(GameTestHelper helper) {
        // Installed version older than every range: the mod-downgrade case.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.1,1.2)"), "0.9");
        helper.assertTrue("[1.0,1.1)".equals(pick),
                "installed below all ranges should pick the oldest generation, got " + pick);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestGapResolvesToOlderNeighbor(GameTestHelper helper) {
        // Version ordering has no distance metric, so a gap resolves by ordering: older neighbor wins.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.3,1.4)"), "1.2");
        helper.assertTrue("[1.0,1.1)".equals(pick),
                "a gap between generations should resolve to the older neighbor, got " + pick);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nearestHandlesExactAndMalformedDeclarations(GameTestHelper helper) {
        // Exact versions are point ranges; malformed declarations are skipped, as in matches().
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.20.1", "1.0.0"), "2.0");
        helper.assertTrue("1.0.0".equals(pick),
                "an exact version should be usable and a malformed range skipped, got " + pick);
        helper.assertTrue(VersionRangeMatcher.nearestVersion(List.of("[oops", ""), "2.0") == null,
                "all-malformed declarations should yield no pick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void entirelyBelowSplitsStaleFromDowngrade(GameTestHelper helper) {
        // Drives the fallback's log level: below = stale datapack (error), not below = downgrade (warn).
        helper.assertTrue(VersionRangeMatcher.isEntirelyBelow("[1.0,1.1)", "1.2"),
                "a range wholly under the installed version is entirely below it");
        helper.assertTrue(!VersionRangeMatcher.isEntirelyBelow("[1.3,1.4)", "1.2"),
                "a range above the installed version is not entirely below it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void shorterVersionPadsWithZero(GameTestHelper helper) {
        // 1.20 and 1.20.0 compare equal (missing trailing tokens pad to zero).
        helper.assertTrue(VersionRangeMatcher.matches("[1.20,1.21)", "1.20.0"), "1.20.0 should sit on the 1.20 lower bound");
        helper.assertTrue(VersionRangeMatcher.matches("1.20", "1.20"), "1.20 should exactly match 1.20");
        helper.succeed();
    }
}
