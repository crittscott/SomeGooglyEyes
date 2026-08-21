package com.github.crittscott.somegoogly.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.util.Map;

/**
 * Compensates for the young-only whole-model scale and translation used by supported Alex's Mobs
 * Citadel models. These transforms surround the model's part rendering and are therefore absent from
 * {@code CitadelResolver}'s per-box transform replay; {@link #preTransform} applies the missing wrapper
 * before resolver traversal.
 *
 * <p>The table is keyed by fully qualified model class name to avoid a compile-time or classloading
 * dependency on Alex's Mobs. Models absent from the table receive no compatibility transform. Per-box
 * scaling and age-specific model classes remain the responsibility of the resolver and datapack
 * definitions, respectively. {@code ModelKangaroo}'s {@code renderOnlyHead} path is not supported by
 * this whole-model compensation.
 */
public final class AlexsMobsCompat {

    /** A young-only whole-model transform: uniform scale, then translate — the model's exact op order. */
    private record Transform(float scale, float tx, float ty, float tz) {
        void apply(PoseStack poseStack) {
            poseStack.scale(scale, scale, scale);
            poseStack.translate(tx, ty, tz);
        }
    }

    private static final String PKG = "com.github.alexthe666.alexsmobs.client.model.";

    /** Create a supported transform whose horizontal translation is zero. */
    private static Transform wrap(float scale, float ty, float tz) {
        return new Transform(scale, 0.0F, ty, tz);
    }

    private static final Map<String, Transform> YOUNG_TRANSFORMS = Map.ofEntries(
            entry("ModelAlligatorSnappingTurtle", wrap(0.25F, 4.5F, 0.125F)),
            entry("ModelAnteater", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelBaldEagle", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelBananaSlug", wrap(0.65F, 0.8F, 0.125F)),
            entry("ModelBlueJay", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelCachalotWhale", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelCaiman", wrap(0.25F, 4.5F, 0.125F)),
            entry("ModelCapuchinMonkey", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelCockroach", wrap(0.65F, 0.815F, 0.125F)),
            entry("ModelCosmaw", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelCrocodile", wrap(0.15F, 8.5F, 0.125F)),
            entry("ModelCrow", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelElephant", wrap(0.35F, 2.8F, 0.0F)),
            entry("ModelEmu", wrap(0.35F, 2.8F, 0.0F)),
            entry("ModelEndergrade", wrap(0.35F, 2.75F, 0.125F)),
            entry("ModelFly", wrap(0.65F, 0.95F, 0.125F)),
            entry("ModelFroststalker", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelGazelle", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelGrizzlyBear", wrap(0.35F, 2.75F, 0.125F)),
            entry("ModelHummingbird", wrap(0.35F, 2.75F, 0.125F)),
            entry("ModelJerboa", wrap(0.65F, 0.815F, 0.125F)),
            entry("ModelKangaroo", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelKomodoDragon", wrap(0.35F, 2.75F, 0.125F)),
            entry("ModelLaviathan", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelLeafcutterAnt", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelManedWolf", wrap(0.65F, 1.0F, 0.125F)),
            entry("ModelMantisShrimp", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelMoose", wrap(0.35F, 2.25F, 0.125F)),
            entry("ModelMudskipper", wrap(0.5F, 1.4F, 0.0F)),
            entry("ModelMungus", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelOrca", wrap(0.35F, 2.75F, 0.125F)),
            entry("ModelPlatypus", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelPotoo", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelRaccoon", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelRainFrog", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelRattlesnake", wrap(0.35F, 2.75F, 0.125F)),
            entry("ModelRhinoceros", wrap(0.5F, 1.3F, 0.0F)),
            entry("ModelRoadrunner", wrap(0.5F, 1.5F, 0.125F)),
            entry("ModelSeagull", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelSeal", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelSkunk", wrap(0.65F, 0.815F, 0.125F)),
            entry("ModelSnowLeopard", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelSugarGlider", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelTasmanianDevil", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelTerrapin", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelTiger", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelToucan", wrap(0.5F, 1.5F, 0.0F)),
            entry("ModelTusklin", wrap(0.45F, 1.6F, 0.125F)),
            entry("ModelWarpedToad", wrap(0.35F, 2.75F, 0.125F))
    );

    private static Map.Entry<String, Transform> entry(String modelClass, Transform transform) {
        return Map.entry(PKG + modelClass, transform);
    }

    private AlexsMobsCompat() {
    }

    /**
     * Reproduce the model's whole-model young-only render transform on {@code poseStack}. No-op for
     * every model not in the table and for adults (every entry here renders adults at identity).
     */
    public static void preTransform(EntityModel<?> model, PoseStack poseStack) {
        if (!model.young) {
            return;
        }
        Transform transform = YOUNG_TRANSFORMS.get(model.getClass().getName());
        if (transform != null) {
            transform.apply(poseStack);
        }
    }
}
