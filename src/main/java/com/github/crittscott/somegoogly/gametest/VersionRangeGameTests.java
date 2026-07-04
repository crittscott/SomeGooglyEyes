package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Pure-logic coverage of {@link VersionRangeMatcher#matches}: bracket-range bounds, exact-version
 * matching, the zero-padding equivalence ({@code 1.20} ≡ {@code 1.20.0}), and malformed input. These
 * need no world; they spawn nothing and {@code succeed()} immediately. The matcher gates whether any
 * eye config loads for a namespace, so its bounds behavior is worth pinning.
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
    public static void shorterVersionPadsWithZero(GameTestHelper helper) {
        // 1.20 and 1.20.0 compare equal (missing trailing tokens pad to zero).
        helper.assertTrue(VersionRangeMatcher.matches("[1.20,1.21)", "1.20.0"), "1.20.0 should sit on the 1.20 lower bound");
        helper.assertTrue(VersionRangeMatcher.matches("1.20", "1.20"), "1.20 should exactly match 1.20");
        helper.succeed();
    }
}
