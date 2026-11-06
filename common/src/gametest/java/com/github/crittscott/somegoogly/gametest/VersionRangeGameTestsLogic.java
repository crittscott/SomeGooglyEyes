package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/**
 * Pure-logic coverage of {@link VersionRangeMatcher}: {@code matches} (bracket-range bounds,
 * exact-version matching, the zero-padding equivalence {@code 1.20} ≡ {@code 1.20.0}, malformed input)
 * and the nearest-generation fallback ({@code nearestVersion}) used when no
 * entry matches the installed version. These need no world; they spawn nothing and {@code succeed()}
 * immediately. The matcher gates whether (and which) eye config loads for a namespace, so its bounds
 * behavior is worth pinning.
 *
 * <p>The in-game steps below run on Minecraft 1.20.1 with {@code entityOverrides = ["minecraft:zombie,100"]}
 * and a datapack whose {@code data/minecraft/eyes/zombie.json} holds only the listed adult entries, each
 * with its own iris color; after {@code /reload}, newly spawned zombies show which entry loaded.
 */
public final class VersionRangeGameTestsLogic {

    private VersionRangeGameTestsLogic() {
    }

    /**
     * An exact version matches only itself. In game: entries {@code "1.20"} (blue) and {@code "1.20.1"}
     * (red); zombies spawn with red irises.
     */
    public static void exactVersionMatchesOnlyItself(GameTestHelper helper) {
        helper.assertTrue(VersionRangeMatcher.matches("1.20.1", "1.20.1"), "exact version should match itself");
        helper.assertTrue(!VersionRangeMatcher.matches("1.20.1", "1.20.2"), "exact version should not match a different one");
        helper.succeed();
    }

    /**
     * A malformed range or blank version never matches. In game: entries {@code "[1.20"} (blue) and
     * {@code "1.20.1"} (red); the server log reports the invalid range and zombies spawn with red irises.
     */
    public static void malformedRangeDoesNotMatch(GameTestHelper helper) {
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1", "1.20.1"), "range with no closing bracket should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1-1.21)", "1.20.5"), "range with no comma should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("", "1.20.1"), "blank range should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.20.1,1.21)", ""), "blank version should not match");
        helper.succeed();
    }

    /**
     * Square brackets include a bound and parentheses exclude it. In game: entries {@code "[1.20,1.20.1)"}
     * (blue) and {@code "1.20.1"} (red) give red irises; change the first to {@code "[1.20,1.20.1]"} and
     * both match, so the first, blue, wins.
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
     * With every range below the installed version, the newest generation is used. In game: entries
     * {@code "[1.19,1.20)"} (blue) and {@code "[1.20,1.21)"} (red); zombies spawn with red irises and the
     * server log notes that the configs use another version's entry.
     */
    public static void nearestPicksNewestOlderGeneration(GameTestHelper helper) {
        // Installed version newer than every range: the stale-datapack case.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.1,1.2)"), "1.3");
        helper.assertTrue("[1.1,1.2)".equals(pick),
                "installed above all ranges should pick the newest generation, got " + pick);
        helper.succeed();
    }

    /**
     * With every range above the installed version, the oldest generation is used. In game: entries
     * {@code "[1.22,1.23)"} (red) and {@code "[1.23,1.24)"} (blue); zombies spawn with red irises and the
     * server log notes that the configs use another version's entry.
     */
    public static void nearestPicksOldestNewerGenerationOnDowngrade(GameTestHelper helper) {
        // Installed version older than every range: the mod-downgrade case.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.1,1.2)"), "0.9");
        helper.assertTrue("[1.0,1.1)".equals(pick),
                "installed below all ranges should pick the oldest generation, got " + pick);
        helper.succeed();
    }

    /**
     * A version in a gap between ranges uses the older neighbor. In game: entries {@code "[1.19,1.20)"}
     * (red) and {@code "[1.21,1.22)"} (blue); zombies spawn with red irises.
     */
    public static void nearestGapResolvesToOlderNeighbor(GameTestHelper helper) {
        // Version ordering has no distance metric, so a gap resolves by ordering: older neighbor wins.
        String pick = VersionRangeMatcher.nearestVersion(List.of("[1.0,1.1)", "[1.3,1.4)"), "1.2");
        helper.assertTrue("[1.0,1.1)".equals(pick),
                "a gap between generations should resolve to the older neighbor, got " + pick);
        helper.succeed();
    }

    /**
     * The fallback treats exact versions as point ranges and skips malformed ones. In game: entries
     * {@code "[1.20"} (blue) and {@code "1.20.1"} (red); zombies spawn with red irises.
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
     * A shorter version pads with zeros, so {@code 1.20} equals {@code 1.20.0}. In game: entries
     * {@code "[1.19,1.20)"} (blue) and {@code "[1.20.1.0,1.21)"} (red); zombies spawn with red irises.
     */
    public static void shorterVersionPadsWithZero(GameTestHelper helper) {
        // 1.20 and 1.20.0 compare equal (missing trailing tokens pad to zero).
        helper.assertTrue(VersionRangeMatcher.matches("[1.20,1.21)", "1.20.0"), "1.20.0 should sit on the 1.20 lower bound");
        helper.assertTrue(VersionRangeMatcher.matches("1.20", "1.20"), "1.20 should exactly match 1.20");
        helper.succeed();
    }

    /**
     * A segment of any length compares as a number, so a date-stamped build sorts after an older one. In game:
     * entries {@code "[1.0.202601010000,1.0.202701010000)"} (blue) and {@code "[1.1,1.2)"} (red) for a mod at
     * version {@code 1.0.202606010000}; its mob spawns with blue irises.
     */
    public static void longSegmentsCompareAsNumbers(GameTestHelper helper) {
        String range = "[1.0.202601010000,1.0.202701010000)";
        helper.assertTrue(VersionRangeMatcher.matches(range, "1.0.202606010000"), "a build inside the range should match");
        helper.assertTrue(!VersionRangeMatcher.matches(range, "1.0.202501010000"), "an older build should not match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.0.5,1.1)", "1.0.alpha"), "text still sorts below a number");
        helper.succeed();
    }

    /**
     * A loader name in a version says nothing about its age, so {@code 1.0.0-forge} is {@code 1.0.0}. In game:
     * entries {@code "[1.0.0,1.1)"} (blue) and {@code "[0.9,1.0)"} (red) for a mod at version
     * {@code 1.0.0-forge}; its mob spawns with blue irises and the log reports no fallback.
     */
    public static void loaderNamesDoNotChangeAVersion(GameTestHelper helper) {
        helper.assertTrue(VersionRangeMatcher.matches("[1.0.0,1.1)", "1.0.0-forge"), "a suffixed lower bound should match");
        helper.assertTrue(!VersionRangeMatcher.matches("[1.0.0,1.1)", "1.1-fabric"), "a suffixed upper bound should not");
        helper.assertTrue(VersionRangeMatcher.matches("[1.20.1-1.5.2-neoforge,1.21)", "1.20.1-1.5.2-neoforge"),
                "a declared suffix should still match itself");
        helper.assertTrue(VersionRangeMatcher.matches("[7.14.0+1.20.1-forge,7.15)", "7.14.1+1.20.1-forge"),
                "a suffix after a build number should change nothing");
        helper.succeed();
    }

    /**
     * Exports declare another mod's definitions for that mod's current minor release. In game: with an
     * optional mod installed (say version 4.12.4), run {@code /sg exportall}; that mod's files under
     * {@code somegoogly-export} declare {@code "version": "[4.12.4,4.13)"}.
     */
    public static void optionalModVersionRangeSynthesis(GameTestHelper helper) {
        helper.assertTrue("[4.12.4,4.13)".equals(VersionRangeMatcher.rangeFor("4.12.4")),
                "an optional-mod version becomes an inclusive-to-next-minor range");
        helper.assertTrue("banana".equals(VersionRangeMatcher.rangeFor("banana")),
                "an unparseable version falls back to an exact-match entry");
        helper.succeed();
    }
}
