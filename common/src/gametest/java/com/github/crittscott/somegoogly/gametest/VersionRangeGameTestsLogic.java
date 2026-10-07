package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/**
 * Pure-logic coverage of {@link VersionRangeMatcher}: {@code matches} (bracket-range bounds,
 * exact-version matching, the zero-padding equivalence {@code 1.20} ≡ {@code 1.20.0}, malformed input)
 * and the nearest-generation fallback ({@code nearestVersion} / {@code isEntirelyBelow}) used when no
 * entry matches the installed version. These need no world; they spawn nothing and {@code succeed()}
 * immediately. The matcher gates whether (and which) eye config loads for a namespace, so its bounds
 * behavior is worth pinning.
 */
public final class VersionRangeGameTestsLogic {

    private VersionRangeGameTestsLogic() {
    }

    /**
     * An exact version matches only itself. No in-game form; guards which entry of an eye definition loads
     * for the installed game or mod version.
     */
    public static void exactVersionMatchesOnlyItself(GameTestHelper helper) {
        helper.assertTrue(VersionRangeMatcher.matches("1.20.1", "1.20.1"), "exact version should match itself");
        helper.assertTrue(!VersionRangeMatcher.matches("1.20.1", "1.20.2"), "exact version should not match a different one");
        helper.succeed();
    }

    /**
     * A malformed range or blank version never matches. No in-game form; guards that a bad version string in
     * a datapack loads nothing rather than everything.
     */
    public static void malformedRangeDoesNotMatch(GameTestHelper helper) {
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1", "1.20.1"), "range with no closing bracket should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1-1.21)", "1.20.5"), "range with no comma should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("", "1.20.1"), "blank range should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1,1.21)", ""), "blank version should not match");
        helper.succeed();
    }

    /**
     * Square brackets include a bound and parentheses exclude it. No in-game form; guards which entry of an
     * eye definition loads for the installed version.
     */
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

    /**
     * With every range below the installed version, the newest generation is used. No in-game form; guards
     * that an outdated datapack still gives mobs eyes.
     */
    public static void nearestPicksNewestOlderGeneration(GameTestHelper helper) {
        // Installed version newer than every range: the stale-datapack case.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.1,1.2)"), "1.3");
        helper.assertTrue("[1.1,1.2)".equals(pick),
                "installed above all ranges should pick the newest generation, got " + pick);
        helper.succeed();
    }

    /**
     * With every range above the installed version, the oldest generation is used. No in-game form; guards
     * that a datapack written for a newer mod version still gives mobs eyes.
     */
    public static void nearestPicksOldestNewerGenerationOnDowngrade(GameTestHelper helper) {
        // Installed version older than every range: the mod-downgrade case.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.1,1.2)"), "0.9");
        helper.assertTrue("[1.0,1.1)".equals(pick),
                "installed below all ranges should pick the oldest generation, got " + pick);
        helper.succeed();
    }

    /**
     * A version in a gap between ranges uses the older neighbor. No in-game form; guards which fallback entry
     * loads.
     */
    public static void nearestGapResolvesToOlderNeighbor(GameTestHelper helper) {
        // Version ordering has no distance metric, so a gap resolves by ordering: older neighbor wins.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.3,1.4)"), "1.2");
        helper.assertTrue("[1.0,1.1)".equals(pick),
                "a gap between generations should resolve to the older neighbor, got " + pick);
        helper.succeed();
    }

    /**
     * The fallback treats exact versions as point ranges and skips malformed ones. No in-game form; guards
     * that one bad entry does not block the fallback.
     */
    public static void nearestHandlesExactAndMalformedDeclarations(GameTestHelper helper) {
        // Exact versions are point ranges; malformed declarations are skipped, as in matches().
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.20.1", "1.0.0"), "2.0");
        helper.assertTrue("1.0.0".equals(pick),
                "an exact version should be usable and a malformed range skipped, got " + pick);
        helper.assertTrue(VersionRangeMatcher.nearestVersion(List.of("[oops", ""), "2.0") == null,
                "all-malformed declarations should yield no pick");
        helper.succeed();
    }

    /**
     * Whether a range lies wholly below the installed version, which sets the fallback's log level. No
     * in-game form; guards that an outdated datapack is logged as an error and a downgrade as a warning.
     */
    public static void entirelyBelowSplitsStaleFromDowngrade(GameTestHelper helper) {
        // Drives the fallback's log level: below = stale datapack (error), not below = downgrade (warn).
        helper.assertTrue(VersionRangeMatcher.isEntirelyBelow("[1.0,1.1)", "1.2"),
                "a range wholly under the installed version is entirely below it");
        helper.assertTrue(!VersionRangeMatcher.isEntirelyBelow("[1.3,1.4)", "1.2"),
                "a range above the installed version is not entirely below it");
        helper.succeed();
    }

    /**
     * A shorter version pads with zeros, so {@code 1.20} equals {@code 1.20.0}. No in-game form; guards
     * version matching for mods that omit a patch number.
     */
    public static void shorterVersionPadsWithZero(GameTestHelper helper) {
        // 1.20 and 1.20.0 compare equal (missing trailing tokens pad to zero).
        helper.assertTrue(VersionRangeMatcher.matches("[1.20,1.21)", "1.20.0"), "1.20.0 should sit on the 1.20 lower bound");
        helper.assertTrue(VersionRangeMatcher.matches("1.20", "1.20"), "1.20 should exactly match 1.20");
        helper.succeed();
    }
}
