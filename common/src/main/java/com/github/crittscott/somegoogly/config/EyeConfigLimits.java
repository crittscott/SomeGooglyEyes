package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ANY;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

/** Shared semantic and work-budget validation for authored and synchronized eye geometry. */
public final class EyeConfigLimits {

    private static final float MAX_ABS_ANGLE = 3_600.0F;
    private static final double MAX_ABS_POSITION = 64.0;
    /** Longest attach-point token a head may name. */
    public static final int MAX_ATTACH_TOKEN_LENGTH = 128;
    /** Most entity types one eye-definition sync may carry. */
    public static final int MAX_CONFIGS_PER_SYNC = 2_048;
    /** Most eyes one head may declare. */
    public static final int MAX_EYES_PER_HEAD = 16;
    /** Most eyes one variant may declare across all its heads. */
    public static final int MAX_EYES_PER_VARIANT = 128;
    /** Most heads one variant may declare. */
    public static final int MAX_HEADS_PER_VARIANT = 32;
    private static final float MAX_SCALE = 16.0F;
    /** Most eyes one eye-definition sync may carry in total. */
    public static final int MAX_TOTAL_EYES_PER_SYNC = 4_096;
    private static final double MAX_VARIANT_WEIGHT = 1_000_000.0;
    /** Most placement variants one definition may declare. */
    public static final int MAX_VARIANTS_PER_CONFIG = 16;

    private EyeConfigLimits() {
    }

    /** A validation reason with localizable feedback and a language-independent log diagnostic. */
    public record Issue(String key, List<Object> arguments, String diagnostic) {
        public Issue {
            arguments = List.copyOf(arguments);
        }

        /** The complete translatable message, including nested entity and placement context. */
        public Component message() {
            Object[] translated = arguments.stream()
                    .map(value -> value instanceof Issue nested ? nested.message() : value).toArray();
            return Component.translatable(key, translated);
        }
    }

    private static final class Budget {
        private int totalEyes;
    }

    private static boolean finiteBounded(double value, double bound) {
        return Double.isFinite(value) && Math.abs(value) <= bound;
    }

    private static boolean finiteRange(float value, float min, float max) {
        return Float.isFinite(value) && value >= min && value <= max;
    }

    private static Issue issue(String reason, String diagnosticFormat, Object... arguments) {
        Object[] diagnostics = Arrays.stream(arguments)
                .map(value -> value instanceof Issue nested ? nested.diagnostic() : value).toArray();
        return new Issue("somegoogly.validation." + reason, List.of(arguments),
                String.format(Locale.ROOT, diagnosticFormat, diagnostics));
    }

    private static Issue validateConfigSet(RuntimeConfigSet set, Budget budget) {
        if (set == null || !set.hasAnyConfig()) {
            return issue("no_age", "config set has no age configuration");
        }
        Issue error = validateOptional(AGE_ADULT, set.adult, budget);
        if (error != null) {
            return error;
        }
        error = validateOptional(AGE_BABY, set.baby, budget);
        if (error != null) {
            return error;
        }
        return validateOptional(AGE_ANY, set.any, budget);
    }

    private static Issue validateEye(EyeDefinition eye, int eyeIndex, int eyeCount) {
        EyePlacement placement = eye.placement();
        if (!finiteBounded(placement.position().x, MAX_ABS_POSITION)
                || !finiteBounded(placement.position().y, MAX_ABS_POSITION)
                || !finiteBounded(placement.position().z, MAX_ABS_POSITION)) {
            return issue("position", "position is not finite or is out of range");
        }
        if (!finiteRange(placement.eyeScale(), 0.0F, MAX_SCALE)
                || !finiteRange(placement.irisScale(), 0.0F, MAX_SCALE)
                || !finiteRange(placement.depth(), 0.0F, MAX_SCALE)) {
            return issue("scale_depth", "scale or depth is not finite or is out of range");
        }
        if (!finiteBounded(placement.inclination(), MAX_ABS_ANGLE)
                || !finiteBounded(placement.azimuth(), MAX_ABS_ANGLE)) {
            return issue("angle", "angle is not finite or is out of range");
        }
        int crossTarget = placement.crossTarget();
        if (crossTarget != EyePlacement.NO_CROSS_TARGET
                && (crossTarget < 0 || crossTarget >= eyeCount || crossTarget == eyeIndex)) {
            return issue("cross_target", "cross-eye target is not another eye in the same head");
        }
        return null;
    }

    private static Issue validateOptional(String age, @Nullable RuntimeConfig config, Budget budget) {
        if (config == null) {
            return null;
        }
        Issue error = validateRuntimeConfig(config, budget);
        return error == null ? null : issue("age_context", "%s: %s", issue("age_" + age, age), error);
    }

    /** Return a structured validation issue, or {@code null} when one runtime config is safe. */
    @Nullable
    public static Issue validateRuntimeConfig(RuntimeConfig config) {
        return validateRuntimeConfig(config, new Budget());
    }

    private static Issue validateRuntimeConfig(RuntimeConfig config, Budget budget) {
        if (config.variants.size() > MAX_VARIANTS_PER_CONFIG) {
            return issue("variant_count", "variant count %s exceeds %s", config.variants.size(), MAX_VARIANTS_PER_CONFIG);
        }
        if (config.enabled && config.variants.isEmpty()) {
            return issue("no_variants", "enabled config has no variants");
        }

        double totalWeight = 0.0;
        for (int variantIndex = 0; variantIndex < config.variants.size(); variantIndex++) {
            Variant variant = config.variants.get(variantIndex);
            if (!Double.isFinite(variant.weight) || variant.weight < 0.0 || variant.weight > MAX_VARIANT_WEIGHT) {
                return issue("invalid_weight", "variant %s has invalid weight", variantIndex);
            }
            totalWeight += variant.weight;
            if (variant.heads.isEmpty()) {
                return issue("no_heads", "variant %s has no heads", variantIndex);
            }
            if (variant.heads.size() > MAX_HEADS_PER_VARIANT) {
                return issue("head_count", "variant %s head count exceeds %s", variantIndex, MAX_HEADS_PER_VARIANT);
            }

            int variantEyes = 0;
            for (int headIndex = 0; headIndex < variant.heads.size(); headIndex++) {
                HeadConfig head = variant.heads.get(headIndex);
                if (head.attachPoint == null || head.attachPoint.isBlank()
                        || head.attachPoint.length() > MAX_ATTACH_TOKEN_LENGTH) {
                    return issue("invalid_attachment", "variant %s head %s has invalid attachment token", variantIndex, headIndex);
                }
                if (head.eyes.isEmpty()) {
                    return issue("no_eyes", "variant %s head %s has no eyes", variantIndex, headIndex);
                }
                if (head.eyes.size() > MAX_EYES_PER_HEAD) {
                    return issue("head_eye_count", "variant %s head %s eye count exceeds %s", variantIndex, headIndex, MAX_EYES_PER_HEAD);
                }
                variantEyes += head.eyes.size();
                if (variantEyes > MAX_EYES_PER_VARIANT) {
                    return issue("variant_eye_count", "variant %s eye count exceeds %s", variantIndex, MAX_EYES_PER_VARIANT);
                }
                budget.totalEyes += head.eyes.size();
                if (budget.totalEyes > MAX_TOTAL_EYES_PER_SYNC) {
                    return issue("total_eye_count", "total eye count exceeds %s", MAX_TOTAL_EYES_PER_SYNC);
                }
                for (int eyeIndex = 0; eyeIndex < head.eyes.size(); eyeIndex++) {
                    Issue error = validateEye(head.eyes.get(eyeIndex), eyeIndex, head.eyes.size());
                    if (error != null) {
                        return issue("eye_context", "variant %s head %s eye %s: %s", variantIndex, headIndex, eyeIndex, error);
                    }
                }
            }
        }
        if (config.enabled && totalWeight <= 0.0) {
            return issue("no_positive_weight", "enabled config has no positive variant weight");
        }
        return null;
    }

    /** Return a structured validation issue, or {@code null} when the complete sync is safe. */
    @Nullable
    public static Issue validateSync(Map<ResourceLocation, RuntimeConfigSet> configs) {
        if (configs.size() > MAX_CONFIGS_PER_SYNC) {
            return issue("config_count", "entity config count %s exceeds %s", configs.size(), MAX_CONFIGS_PER_SYNC);
        }
        Budget budget = new Budget();
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : configs.entrySet()) {
            Issue error = validateConfigSet(entry.getValue(), budget);
            if (error != null) {
                return issue("entity_context", "%s: %s", entry.getKey().toString(), error);
            }
        }
        return null;
    }
}
