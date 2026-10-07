package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ANY;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

/** Shared semantic and work-budget validation for authored and synchronized eye geometry. */
public final class EyeConfigLimits {

    public static final int MAX_CONFIGS_PER_SYNC = 2_048;
    public static final int MAX_VARIANTS_PER_CONFIG = 16;
    public static final int MAX_HEADS_PER_VARIANT = 32;
    public static final int MAX_EYES_PER_HEAD = 16;
    public static final int MAX_EYES_PER_VARIANT = 128;
    public static final int MAX_TOTAL_EYES_PER_SYNC = 4_096;
    public static final int MAX_ATTACH_TOKEN_LENGTH = 128;

    private static final double MAX_VARIANT_WEIGHT = 1_000_000.0;
    private static final double MAX_ABS_POSITION = 64.0;
    private static final float MAX_SCALE = 16.0F;
    private static final float MAX_ABS_ANGLE = 3_600.0F;

    private EyeConfigLimits() {
    }

    /** Return a concise problem description, or {@code null} when the complete sync is safe. */
    @Nullable
    public static String validateSync(Map<ResourceLocation, RuntimeConfigSet> configs) {
        if (configs.size() > MAX_CONFIGS_PER_SYNC) {
            return "entity config count " + configs.size() + " exceeds " + MAX_CONFIGS_PER_SYNC;
        }
        Budget budget = new Budget();
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : configs.entrySet()) {
            String error = validateConfigSet(entry.getValue(), budget);
            if (error != null) {
                return entry.getKey() + ": " + error;
            }
        }
        return null;
    }

    /** Return a concise problem description, or {@code null} when one runtime config is safe. */
    @Nullable
    public static String validateRuntimeConfig(RuntimeConfig config) {
        return validateRuntimeConfig(config, new Budget());
    }

    private static String validateConfigSet(RuntimeConfigSet set, Budget budget) {
        if (set == null || !set.hasAnyConfig()) {
            return "config set has no age configuration";
        }
        String error = validateOptional(AGE_ADULT, set.adult, budget);
        if (error != null) {
            return error;
        }
        error = validateOptional(AGE_BABY, set.baby, budget);
        if (error != null) {
            return error;
        }
        return validateOptional(AGE_ANY, set.any, budget);
    }

    private static String validateOptional(String age, @Nullable RuntimeConfig config, Budget budget) {
        if (config == null) {
            return null;
        }
        String error = validateRuntimeConfig(config, budget);
        return error == null ? null : age + ": " + error;
    }

    private static String validateRuntimeConfig(RuntimeConfig config, Budget budget) {
        if (config.variants.size() > MAX_VARIANTS_PER_CONFIG) {
            return "variant count " + config.variants.size() + " exceeds " + MAX_VARIANTS_PER_CONFIG;
        }
        if (config.enabled && config.variants.isEmpty()) {
            return "enabled config has no variants";
        }

        double totalWeight = 0.0;
        for (int variantIndex = 0; variantIndex < config.variants.size(); variantIndex++) {
            Variant variant = config.variants.get(variantIndex);
            if (!Double.isFinite(variant.weight) || variant.weight < 0.0 || variant.weight > MAX_VARIANT_WEIGHT) {
                return "variant " + variantIndex + " has invalid weight";
            }
            totalWeight += variant.weight;
            if (!Double.isFinite(totalWeight)) {
                return "variant weights overflow";
            }
            if (variant.heads.isEmpty()) {
                return "variant " + variantIndex + " has no heads";
            }
            if (variant.heads.size() > MAX_HEADS_PER_VARIANT) {
                return "variant " + variantIndex + " head count exceeds " + MAX_HEADS_PER_VARIANT;
            }

            int variantEyes = 0;
            for (int headIndex = 0; headIndex < variant.heads.size(); headIndex++) {
                HeadConfig head = variant.heads.get(headIndex);
                if (head.attachPoint == null || head.attachPoint.isBlank()
                        || head.attachPoint.length() > MAX_ATTACH_TOKEN_LENGTH) {
                    return "variant " + variantIndex + " head " + headIndex + " has invalid attachment token";
                }
                if (head.eyes.isEmpty()) {
                    return "variant " + variantIndex + " head " + headIndex + " has no eyes";
                }
                if (head.eyes.size() > MAX_EYES_PER_HEAD) {
                    return "variant " + variantIndex + " head " + headIndex + " eye count exceeds "
                            + MAX_EYES_PER_HEAD;
                }
                variantEyes += head.eyes.size();
                if (variantEyes > MAX_EYES_PER_VARIANT) {
                    return "variant " + variantIndex + " eye count exceeds " + MAX_EYES_PER_VARIANT;
                }
                budget.totalEyes += head.eyes.size();
                if (budget.totalEyes > MAX_TOTAL_EYES_PER_SYNC) {
                    return "total eye count exceeds " + MAX_TOTAL_EYES_PER_SYNC;
                }
                for (int eyeIndex = 0; eyeIndex < head.eyes.size(); eyeIndex++) {
                    String error = validateEye(head.eyes.get(eyeIndex), eyeIndex, head.eyes.size());
                    if (error != null) {
                        return "variant " + variantIndex + " head " + headIndex + " eye " + eyeIndex
                                + ": " + error;
                    }
                }
            }
        }
        if (config.enabled && totalWeight <= 0.0) {
            return "enabled config has no positive variant weight";
        }
        return null;
    }

    private static String validateEye(EyeDefinition eye, int eyeIndex, int eyeCount) {
        EyePlacement placement = eye.placement();
        if (!finiteBounded(placement.position().x, MAX_ABS_POSITION)
                || !finiteBounded(placement.position().y, MAX_ABS_POSITION)
                || !finiteBounded(placement.position().z, MAX_ABS_POSITION)) {
            return "position is not finite or is out of range";
        }
        if (!finiteRange(placement.eyeScale(), 0.0F, MAX_SCALE)
                || !finiteRange(placement.irisScale(), 0.0F, MAX_SCALE)
                || !finiteRange(placement.depth(), 0.0F, MAX_SCALE)) {
            return "scale or depth is not finite or is out of range";
        }
        if (!finiteBounded(placement.inclination(), MAX_ABS_ANGLE)
                || !finiteBounded(placement.azimuth(), MAX_ABS_ANGLE)) {
            return "angle is not finite or is out of range";
        }
        int crossTarget = placement.crossTarget();
        if (crossTarget != EyePlacement.NO_CROSS_TARGET
                && (crossTarget < 0 || crossTarget >= eyeCount || crossTarget == eyeIndex)) {
            return "cross-eye target is not another eye in the same head";
        }
        return null;
    }

    private static boolean finiteBounded(double value, double bound) {
        return Double.isFinite(value) && Math.abs(value) <= bound;
    }

    private static boolean finiteRange(float value, float min, float max) {
        return Float.isFinite(value) && value >= min && value <= max;
    }

    private static final class Budget {
        private int totalEyes;
    }
}
