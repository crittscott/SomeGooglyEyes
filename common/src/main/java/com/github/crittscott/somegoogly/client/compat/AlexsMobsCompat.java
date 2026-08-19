package com.github.crittscott.somegoogly.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.util.Map;

/**
 * Compensates for Alex's Mobs' whole-model baby render transforms. Most of that mod's Citadel
 * ({@code AdvancedEntityModel}) models wrap their entire part list in a young-only
 * {@code pushPose -> scale -> translate} inside {@code renderToBuffer}, applied and popped outside any
 * individual box's own transform — invisible to {@code CitadelResolver}'s chain replay, which only
 * replays each box's own {@code translateAndRotate}. {@link #preTransform} reproduces that wrap on the
 * pose stack before the resolver walk, the same mechanism {@link ExoticBirdsCompat} uses for that mod's
 * birds.
 *
 * <p>A model's own per-box {@code setScale()} calls (e.g. Grizzly Bear's baby head) are <b>not</b>
 * represented here — those are already visible to the resolver, since a box's live scale is part of what
 * {@code translateAndRotate} replays. Only the surrounding whole-model wrap is invisible.
 *
 * <p>Covers every {@code Animal}/{@code TamableAnimal} Alex's Mobs model confirmed to use this exact
 * shape, whether or not this mod currently ships an eye definition for that entity. Checked and
 * deliberately excluded:
 * <ul>
 *   <li>{@code ModelGorilla} — scales its baby head but has no surrounding {@code pushPose}/{@code scale}/
 *       {@code translate} wrap at all; nothing to compensate for.</li>
 *   <li>{@code ModelGeladaMonkey} — a different bug: {@code neck} and its child {@code head} are each
 *       independently scaled without {@code setShouldScaleChildren(true)} on {@code neck}, so the chain
 *       replay double-compounds a scale Citadel's own render would have undone. Not a whole-model wrap.</li>
 *   <li>{@code ModelBisonBaby}/{@code ModelTarantulaHawkBaby} — Bison and Tarantula Hawk render babies
 *       with an entirely separate model class instead of a wrap; needs an age-specific datapack entry, not
 *       a resolver-side transform.</li>
 * </ul>
 * {@code ModelKangaroo}'s young branch additionally has a {@code renderOnlyHead} conditional (likely the
 * joey-in-pouch render) not investigated here; this entry covers its normal render path only.
 *
 * <p>Keyed by model class name, so there is no compile-time or classloading dependency on Alex's Mobs.
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

    /** {@code tx} is 0 for every model checked so far; {@code wrap} bakes that in. */
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
