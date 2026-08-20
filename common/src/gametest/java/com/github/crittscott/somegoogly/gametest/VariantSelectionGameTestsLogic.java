package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/**
 * Pure-logic coverage of {@link HeadInfo#chooseVariantIndex} and {@link Variant#weight()} — the
 * deterministic weighted pick that lets the client and server agree on a mob's arrangement from its
 * stored roll without sending an index. World-less: builds configs in memory and asserts. The
 * cumulative-weight boundary cases are the load-bearing part (a drift here desyncs viewers).
 */
public final class VariantSelectionGameTestsLogic {

    private VariantSelectionGameTestsLogic() {
    }

    private static RuntimeConfig configOf(double... weights) {
        RuntimeConfig config = new RuntimeConfig();
        config.enabled = true;
        Variant[] variants = new Variant[weights.length];
        for (int i = 0; i < weights.length; i++) {
            variants[i] = variantOf(weights[i]);
        }
        config.variants = List.of(variants);
        return config;
    }

    private static Variant variantOf(double weight) {
        Variant variant = new Variant();
        variant.weight = weight;
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(EyeDefinition.DEFAULT);
        variant.heads = List.of(head);
        return variant;
    }

    public static void cumulativeWeightBoundariesPickExpectedVariant(GameTestHelper helper) {
        // Weights 1 and 3 → total 4. Variant 0 owns roll in [0, 0.25), variant 1 owns [0.25, 1).
        RuntimeConfig config = configOf(1.0, 3.0);
        helper.assertTrue(HeadInfo.chooseVariantIndex(config, 0.0F) == 0, "roll 0.0 → variant 0");
        helper.assertTrue(HeadInfo.chooseVariantIndex(config, 0.24F) == 0, "roll just below the boundary → variant 0");
        helper.assertTrue(HeadInfo.chooseVariantIndex(config, 0.25F) == 1, "roll on the boundary → variant 1");
        helper.assertTrue(HeadInfo.chooseVariantIndex(config, 0.99F) == 1, "roll near 1.0 → last variant");
        helper.succeed();
    }

    public static void degenerateConfigsFallToFirstVariant(GameTestHelper helper) {
        helper.assertTrue(HeadInfo.chooseVariantIndex(null, 0.5F) == 0, "null config → index 0");

        RuntimeConfig empty = new RuntimeConfig();
        empty.enabled = true;
        empty.variants = List.of();
        helper.assertTrue(HeadInfo.chooseVariantIndex(empty, 0.5F) == 0, "no variants → index 0");

        // All-zero weights (here a single zero-weight variant) leave nothing to pick → index 0.
        helper.assertTrue(HeadInfo.chooseVariantIndex(configOf(0.0), 0.5F) == 0, "zero total weight → index 0");
        helper.succeed();
    }

    public static void rollIsDeterministicForAConfig(GameTestHelper helper) {
        RuntimeConfig config = configOf(2.0, 1.0, 1.0);
        for (float roll = 0F; roll < 1F; roll += 0.05F) {
            int first = HeadInfo.chooseVariantIndex(config, roll);
            int second = HeadInfo.chooseVariantIndex(config, roll);
            helper.assertTrue(first == second, "same roll must always pick the same variant (roll " + roll + ")");
        }
        helper.succeed();
    }

    public static void weightDefaultsAndClamping(GameTestHelper helper) {
        // weight is a required field on disk, so a fresh Variant carries the in-memory default rather
        // than an "absent" marker; only the negative clamp is a runtime concern.
        helper.assertTrue(new Variant().weight() == 1.0, "a fresh variant weighs 1.0");

        Variant negative = new Variant();
        negative.weight = -5.0;
        helper.assertTrue(negative.weight() == 0.0, "negative weight clamps to 0.0");
        helper.succeed();
    }
}
