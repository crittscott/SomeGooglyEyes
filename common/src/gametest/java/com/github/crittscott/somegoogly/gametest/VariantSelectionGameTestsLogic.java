package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

/**
 * Pure-logic coverage of {@link EyeConfigModel#chooseVariantIndex} — the
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

    /**
     * A stored roll picks the variant whose share of the cumulative weight contains it. No in-game form;
     * guards that every client places a mob's eyes in the same arrangement the server chose.
     */
    public static void cumulativeWeightBoundariesPickExpectedVariant(GameTestHelper helper) {
        // Weights 1 and 3 → total 4. Variant 0 owns roll in [0, 0.25), variant 1 owns [0.25, 1).
        RuntimeConfig config = configOf(1.0, 3.0);
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(config, 0.0F) == 0, "roll 0.0 → variant 0");
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(config, 0.24F) == 0, "roll just below the boundary → variant 0");
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(config, 0.25F) == 1, "roll on the boundary → variant 1");
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(config, 0.99F) == 1, "roll near 1.0 → last variant");
        helper.succeed();
    }

    /**
     * A missing, empty, or zero-weight definition picks the first variant. No in-game form; guards that such
     * a definition never breaks the pick.
     */
    public static void degenerateConfigsFallToFirstVariant(GameTestHelper helper) {
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(null, 0.5F) == 0, "null config → index 0");

        RuntimeConfig empty = new RuntimeConfig();
        empty.enabled = true;
        empty.variants = List.of();
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(empty, 0.5F) == 0, "no variants → index 0");

        // All-zero weights (here a single zero-weight variant) leave nothing to pick → index 0.
        helper.assertTrue(EyeConfigModel.chooseVariantIndex(configOf(0.0), 0.5F) == 0, "zero total weight → index 0");
        helper.succeed();
    }

    /**
     * The same roll always picks the same variant. No in-game form; guards that a mob keeps its arrangement
     * across reloads and for every viewer.
     */
    public static void rollIsDeterministicForAConfig(GameTestHelper helper) {
        RuntimeConfig config = configOf(2.0, 1.0, 1.0);
        for (float roll = 0F; roll < 1F; roll += 0.05F) {
            int first = EyeConfigModel.chooseVariantIndex(config, roll);
            int second = EyeConfigModel.chooseVariantIndex(config, roll);
            helper.assertTrue(first == second, "same roll must always pick the same variant (roll " + roll + ")");
        }
        helper.succeed();
    }
}
