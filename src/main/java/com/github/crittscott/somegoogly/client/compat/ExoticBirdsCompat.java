package com.github.crittscott.somegoogly.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.util.Map;

/**
 * Compensates for Exotic Birds' whole-model render transforms. Every bird model in that mod
 * overrides {@code renderToBuffer} and wraps its part rendering in a model-wide
 * {@code pushPose → scale → translate} (the models are authored oversized and shrunk at draw time;
 * babies always, adults on most models). That transform lives only inside the model's own
 * push/pop, so replaying the part tree via {@code translateAndRotate} — which is all a resolver can
 * see — places eyes in the unscaled space: wrong pivots, motion arcs too large, everything shifted.
 *
 * <p>{@link #preTransform} applies the same scale + translate to the pose stack before the resolver
 * walk, reproducing the model's rendered space exactly (the model applies its transform before any
 * part's {@code translateAndRotate}, so pre-multiplying composes identically). Call it after the
 * layer's {@code pushPose}, before {@code toAttachmentSpace}; it is a no-op for every other model.
 *
 * <p>The constants are baked from the bytecode of {@code exoticbirds-1.20.1-1.0.0.jar} (the mod is
 * closed-source; they are hardcoded in each model's {@code renderToBuffer}, unreachable by
 * reflection). Keyed by model class name, so there is no compile-time or classloading dependency.
 * Models absent an adult entry render adults untransformed — those birds already worked. Flamingo's
 * per-part lower-beak wrapper (a z-fight nudge) is deliberately not represented. If Exotic Birds
 * ever updates, re-extract: the table is version-specific to that jar.
 */
public final class ExoticBirdsCompat {

    /** One branch's whole-model transform: uniform scale, then translate — the model's exact op order. */
    private record Transform(float scale, float tx, float ty, float tz) {
        void apply(PoseStack poseStack) {
            poseStack.scale(scale, scale, scale);
            poseStack.translate(tx, ty, tz);
        }
    }

    /** Per-model transforms; {@code adult} is {@code null} where the adult branch renders at identity. */
    private record Entry(Transform adult, Transform baby) {
    }

    private static final String PKG = "net.pavocado.exoticbirds.client.model.";

    private static final Map<String, Entry> TRANSFORMS = Map.ofEntries(
            entry("BoobyModel", adult(0.9F, 0.15F), baby(0.5F, 1.5F)),
            entry("BudgerigarModel", adult(0.8F, 0.35F), baby(0.4F, 2.23F)),
            entry("CassowaryModel", adult(1.2F, -0.25F), baby(0.8F, 0.4F)),
            entry("CockatooModel", adult(0.95F, 0.09F), baby(0.5F, 1.5F)),
            entry("CraneModel", adult(0.8F, 0.35F), baby(0.4F, 2.05F)),
            entry("DuckModel", null, baby(0.55F, 1.2F)),
            entry("FlamingoModel", null, baby(0.6F, 1.0F)),
            entry("GouldianFinchModel", adult(0.8F, 0.38F), baby(0.5F, 1.5F)),
            entry("GullModel", adult(0.7F, 0.65F), baby(0.45F, 1.8F)),
            entry("HeronModel", adult(0.8F, 0.35F), baby(0.4F, 2.05F)),
            entry("HummingbirdModel", adult(0.3F, 4.0F), baby(0.15F, 9.3F)),
            entry("KingfisherModel", null, baby(0.4F, 2.2F)),
            entry("KiwiModel", null, baby(0.7F, 0.6F)),
            entry("KookaburraModel", null, baby(0.6F, 1.0F)),
            entry("LyrebirdModel", null, baby(0.7F, 0.6F)),
            entry("MacawModel", adult(0.95F, 0.09F), baby(0.5F, 1.5F)),
            entry("MagpieModel", null, baby(0.6F, 1.0F)),
            entry("OstrichModel", adult(1.3F, -0.35F), baby(0.7F, 0.6F)),
            entry("OwlModel", adult(0.8F, 0.35F), baby(0.5F, 1.45F)),
            entry("PeafowlModel", adult(0.85F, 1.1F), baby(0.45F, 2.7F)),
            entry("PelicanModel", adult(1.1F, -0.18F), baby(0.7F, 0.6F)),
            entry("PenguinModel", null, baby(0.5F, 1.5F)),
            entry("PhoenixModel", adult(1.3F, -0.3F), baby(0.7F, 0.6F)),
            entry("PigeonModel", adult(0.8F, 0.34F), baby(0.4F, 2.2F)),
            entry("RoadrunnerModel", adult(0.75F, 0.45F), baby(0.45F, 1.8F)),
            entry("SongbirdModel", null, baby(0.6F, 1.0F)),
            entry("SwanModel", new Transform(1.0F, 0F, 0F, -0.2F), new Transform(0.6F, 0F, 1.0F, -0.2F)),
            entry("ToucanModel", null, baby(0.55F, 1.2F)),
            entry("WoodpeckerModel", new Transform(0.7F, 0F, 0F, -0.2F), new Transform(0.45F, 0F, 1.2F, -0.1F))
    );

    private ExoticBirdsCompat() {
    }

    private static Map.Entry<String, Entry> entry(String modelClass, Transform adult, Transform baby) {
        return Map.entry(PKG + modelClass, new Entry(adult, baby));
    }

    private static Transform adult(float scale, float ty) {
        return new Transform(scale, 0F, ty, 0F);
    }

    private static Transform baby(float scale, float ty) {
        return new Transform(scale, 0F, ty, 0F);
    }

    /**
     * Reproduce the model's whole-model render transform on {@code poseStack}, choosing the branch by
     * {@code model.young} — the same field the bird models branch on. No-op for non-Exotic-Birds
     * models and for adult birds whose adult branch renders at identity.
     */
    public static void preTransform(EntityModel<?> model, PoseStack poseStack) {
        Entry entry = TRANSFORMS.get(model.getClass().getName());
        if (entry == null) {
            return;
        }
        Transform transform = model.young ? entry.baby() : entry.adult();
        if (transform != null) {
            transform.apply(poseStack);
        }
    }
}
