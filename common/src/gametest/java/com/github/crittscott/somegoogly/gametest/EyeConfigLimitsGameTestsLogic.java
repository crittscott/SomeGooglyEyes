package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Semantic-bound coverage for {@link EyeConfigLimits#validateRuntimeConfig(RuntimeConfig)} — the
 * object-graph validator that guards datapack reload and picker export. Its wire-preflight twin
 * ({@code validateWireRuntimeConfig}) is exercised by {@link SerializationGameTestsLogic} and
 * {@link PickerExportGameTestsLogic}; these tests pin the parallel checks on the decoded form so the
 * two cannot drift. Pure and world-less: only {@link GameTestHelper} assertions touch the framework.
 */
public final class EyeConfigLimitsGameTestsLogic {

    private EyeConfigLimitsGameTestsLogic() {
    }

    private static EyeDefinition validEye() {
        return new EyeDefinition(EyePlacement.DEFAULT, EyeAppearance.DEFAULT);
    }

    private static HeadConfig head(EyeDefinition... eyes) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(eyes);
        return head;
    }

    private static HeadConfig head(int eyeCount) {
        EyeDefinition[] eyes = new EyeDefinition[eyeCount];
        for (int i = 0; i < eyeCount; i++) {
            eyes[i] = validEye();
        }
        return head(eyes);
    }

    private static Variant variant(HeadConfig... heads) {
        Variant variant = new Variant();
        variant.heads = List.of(heads);
        return variant;
    }

    private static Variant variant(int headCount, int eyesPerHead) {
        HeadConfig[] heads = new HeadConfig[headCount];
        for (int i = 0; i < headCount; i++) {
            heads[i] = head(eyesPerHead);
        }
        return variant(heads);
    }

    private static RuntimeConfig config(Variant... variants) {
        RuntimeConfig config = new RuntimeConfig();
        config.variants = List.of(variants);
        return config;
    }

    private static RuntimeConfig configWithEye(EyeDefinition eye) {
        return config(variant(head(eye)));
    }

    private static void assertRejected(GameTestHelper helper, RuntimeConfig config, String fragment) {
        String error = EyeConfigLimits.validateRuntimeConfig(config);
        helper.assertTrue(error != null && error.contains(fragment),
                "expected a rejection mentioning '" + fragment + "', got " + error);
    }

    /** {@code crossTarget} must name a different eye in the same head, or be {@link EyePlacement#NO_CROSS_TARGET}. */
    public static void crossTargetMustReferenceAnotherEyeInTheSameHead(GameTestHelper helper) {
        EyePlacement selfTarget = new EyePlacement(Vec3.ZERO, 1F, 1F, 1F,
                EyePlacement.DEFAULT_INCLINATION, EyePlacement.DEFAULT_AZIMUTH, 0);
        assertRejected(helper, configWithEye(new EyeDefinition(selfTarget, EyeAppearance.DEFAULT)),
                "cross-eye target");

        EyePlacement outOfRange = new EyePlacement(Vec3.ZERO, 1F, 1F, 1F,
                EyePlacement.DEFAULT_INCLINATION, EyePlacement.DEFAULT_AZIMUTH, 1);
        assertRejected(helper, configWithEye(new EyeDefinition(outOfRange, EyeAppearance.DEFAULT)),
                "cross-eye target");

        EyePlacement partnered = new EyePlacement(Vec3.ZERO, 1F, 1F, 1F,
                EyePlacement.DEFAULT_INCLINATION, EyePlacement.DEFAULT_AZIMUTH, 1);
        RuntimeConfig ok = config(variant(head(
                new EyeDefinition(partnered, EyeAppearance.DEFAULT), validEye())));
        helper.assertTrue(EyeConfigLimits.validateRuntimeConfig(ok) == null,
                "an eye may cross toward another eye in the same head");
        helper.succeed();
    }

    /** Position, scale/depth, and angle each have a finite hard range. */
    public static void numericPlacementBoundsAreEnforced(GameTestHelper helper) {
        assertRejected(helper, configWithEye(new EyeDefinition(new EyePlacement(
                        new Vec3(100.0, 0.0, 0.0), 1F, 1F, 1F,
                        EyePlacement.DEFAULT_INCLINATION, EyePlacement.DEFAULT_AZIMUTH, -1),
                EyeAppearance.DEFAULT)), "position");
        assertRejected(helper, configWithEye(new EyeDefinition(new EyePlacement(
                        Vec3.ZERO, 64F, 1F, 1F,
                        EyePlacement.DEFAULT_INCLINATION, EyePlacement.DEFAULT_AZIMUTH, -1),
                EyeAppearance.DEFAULT)), "scale or depth");
        assertRejected(helper, configWithEye(new EyeDefinition(new EyePlacement(
                        Vec3.ZERO, 1F, 1F, 1F,
                        EyePlacement.DEFAULT_INCLINATION, 9_000F, -1),
                EyeAppearance.DEFAULT)), "angle");
        helper.succeed();
    }

    /** Every color channel must be finite and within {@code 0..1}. */
    public static void colorChannelsMustBeInRange(GameTestHelper helper) {
        EyeAppearance badIris = new EyeAppearance(EyeColor.WHITE, new EyeColor(2F, 0F, 0F), false);
        assertRejected(helper, configWithEye(new EyeDefinition(EyePlacement.DEFAULT, badIris)), "color channel");
        helper.succeed();
    }

    /** The per-container count caps reject before the object graph is walked. */
    public static void containerCountCapsAreEnforced(GameTestHelper helper) {
        Variant[] tooManyVariants = new Variant[EyeConfigLimits.MAX_VARIANTS_PER_CONFIG + 1];
        for (int i = 0; i < tooManyVariants.length; i++) {
            tooManyVariants[i] = variant(1, 1);
        }
        assertRejected(helper, config(tooManyVariants), "variant count");

        assertRejected(helper, config(variant(EyeConfigLimits.MAX_HEADS_PER_VARIANT + 1, 1)), "head count");
        assertRejected(helper, config(variant(1, EyeConfigLimits.MAX_EYES_PER_HEAD + 1)), "eye count exceeds "
                + EyeConfigLimits.MAX_EYES_PER_HEAD);
        assertRejected(helper, config(variant(9, 15)), "eye count exceeds " + EyeConfigLimits.MAX_EYES_PER_VARIANT);
        helper.succeed();
    }
}
