package com.github.crittscott.somegoogly.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Compensates for whole-model render transforms that supported third-party mob models apply <i>outside</i>
 * their part tree — a model-wide {@code pushPose → scale → translate} wrapped entirely inside the model's
 * own {@code renderToBuffer}. A resolver can only replay {@code translateAndRotate} down the part tree, so
 * without this the eyes land in the model's unscaled space: wrong pivots, oversized motion arcs, offset
 * placement. {@link #preTransform} pre-multiplies the same transform onto the pose stack before the
 * resolver walk (the model applies its transform before any part's {@code translateAndRotate}, so
 * pre-multiplying composes identically). Call it after the layer's {@code pushPose}, before
 * {@code toAttachmentSpace}; it is a no-op for every unlisted model.
 *
 * <p>Each table is keyed by fully qualified model class name, so there is no compile-time or classloading
 * dependency on either mod. The branch is chosen by {@code model.young} — the same field the models
 * themselves branch on. A {@code null} adult transform means that branch renders at identity (those
 * models already worked for adults); the Alex's Mobs entries are young-only and always carry a
 * {@code null} adult.
 *
 * <p><b>Alex's Mobs</b> (Citadel models): young-only whole-model scale + translate. {@code ModelKangaroo}'s
 * {@code renderOnlyHead} path is not covered by this whole-model compensation; per-box scaling and
 * age-specific model classes remain the resolver's and the datapack's responsibility.
 *
 * <p><b>Exotic Birds</b>: the listed model classes use fixed whole-model transforms for every baby and
 * for most adults. Compatibility is limited to model versions that retain those class names and
 * transforms. Flamingo's per-part lower-beak z-fight adjustment is intentionally outside this
 * whole-model table.
 */
public final class ThirdPartyModelWraps {

    /** A whole-model branch transform: uniform scale, then translate — the model's exact op order. */
    private record Transform(float scale, float tx, float ty, float tz) {
        void apply(PoseStack poseStack) {
            poseStack.scale(scale, scale, scale);
            poseStack.translate(tx, ty, tz);
        }
    }

    /** Per-model transforms; {@code adult} is {@code null} where the adult branch renders at identity. */
    private record Entry(@Nullable Transform adult, Transform baby) {
    }

    private static final String ALEXS_MOBS_PKG = "com.github.alexthe666.alexsmobs.client.model.";
    private static final String EXOTIC_BIRDS_PKG = "net.pavocado.exoticbirds.client.model.";

    /** All young-only: uniform scale, then a horizontally centered translate. */
    private static final Map<String, Entry> ALEXS_MOBS = Map.ofEntries(
            am("ModelAlligatorSnappingTurtle", t(0.25F, 4.5F, 0.125F)),
            am("ModelAnteater", t(0.5F, 1.5F, 0.0F)),
            am("ModelBaldEagle", t(0.5F, 1.5F, 0.0F)),
            am("ModelBananaSlug", t(0.65F, 0.8F, 0.125F)),
            am("ModelBlueJay", t(0.5F, 1.5F, 0.0F)),
            am("ModelCachalotWhale", t(0.5F, 1.5F, 0.125F)),
            am("ModelCaiman", t(0.25F, 4.5F, 0.125F)),
            am("ModelCapuchinMonkey", t(0.5F, 1.5F, 0.125F)),
            am("ModelCockroach", t(0.65F, 0.815F, 0.125F)),
            am("ModelCosmaw", t(0.5F, 1.5F, 0.0F)),
            am("ModelCrocodile", t(0.15F, 8.5F, 0.125F)),
            am("ModelCrow", t(0.5F, 1.5F, 0.0F)),
            am("ModelElephant", t(0.35F, 2.8F, 0.0F)),
            am("ModelEmu", t(0.35F, 2.8F, 0.0F)),
            am("ModelEndergrade", t(0.35F, 2.75F, 0.125F)),
            am("ModelFly", t(0.65F, 0.95F, 0.125F)),
            am("ModelFroststalker", t(0.5F, 1.5F, 0.0F)),
            am("ModelGazelle", t(0.5F, 1.5F, 0.125F)),
            am("ModelGrizzlyBear", t(0.35F, 2.75F, 0.125F)),
            am("ModelHummingbird", t(0.35F, 2.75F, 0.125F)),
            am("ModelJerboa", t(0.65F, 0.815F, 0.125F)),
            am("ModelKangaroo", t(0.5F, 1.5F, 0.0F)),
            am("ModelKomodoDragon", t(0.35F, 2.75F, 0.125F)),
            am("ModelLaviathan", t(0.5F, 1.5F, 0.125F)),
            am("ModelLeafcutterAnt", t(0.5F, 1.5F, 0.125F)),
            am("ModelManedWolf", t(0.65F, 1.0F, 0.125F)),
            am("ModelMantisShrimp", t(0.5F, 1.5F, 0.125F)),
            am("ModelMoose", t(0.35F, 2.25F, 0.125F)),
            am("ModelMudskipper", t(0.5F, 1.4F, 0.0F)),
            am("ModelMungus", t(0.5F, 1.5F, 0.125F)),
            am("ModelOrca", t(0.35F, 2.75F, 0.125F)),
            am("ModelPlatypus", t(0.5F, 1.5F, 0.0F)),
            am("ModelPotoo", t(0.5F, 1.5F, 0.125F)),
            am("ModelRaccoon", t(0.5F, 1.5F, 0.0F)),
            am("ModelRainFrog", t(0.5F, 1.5F, 0.125F)),
            am("ModelRattlesnake", t(0.35F, 2.75F, 0.125F)),
            am("ModelRhinoceros", t(0.5F, 1.3F, 0.0F)),
            am("ModelRoadrunner", t(0.5F, 1.5F, 0.125F)),
            am("ModelSeagull", t(0.5F, 1.5F, 0.0F)),
            am("ModelSeal", t(0.5F, 1.5F, 0.0F)),
            am("ModelSkunk", t(0.65F, 0.815F, 0.125F)),
            am("ModelSnowLeopard", t(0.5F, 1.5F, 0.0F)),
            am("ModelSugarGlider", t(0.5F, 1.5F, 0.0F)),
            am("ModelTasmanianDevil", t(0.5F, 1.5F, 0.0F)),
            am("ModelTerrapin", t(0.5F, 1.5F, 0.0F)),
            am("ModelTiger", t(0.5F, 1.5F, 0.0F)),
            am("ModelToucan", t(0.5F, 1.5F, 0.0F)),
            am("ModelTusklin", t(0.45F, 1.6F, 0.125F)),
            am("ModelWarpedToad", t(0.35F, 2.75F, 0.125F))
    );

    private static final Map<String, Entry> EXOTIC_BIRDS = Map.ofEntries(
            eb("BoobyModel", t(0.9F, 0.15F), t(0.5F, 1.5F)),
            eb("BudgerigarModel", t(0.8F, 0.35F), t(0.4F, 2.23F)),
            eb("CassowaryModel", t(1.2F, -0.25F), t(0.8F, 0.4F)),
            eb("CockatooModel", t(0.95F, 0.09F), t(0.5F, 1.5F)),
            eb("CraneModel", t(0.8F, 0.35F), t(0.4F, 2.05F)),
            eb("DuckModel", null, t(0.55F, 1.2F)),
            eb("FlamingoModel", null, t(0.6F, 1.0F)),
            eb("GouldianFinchModel", t(0.8F, 0.38F), t(0.5F, 1.5F)),
            eb("GullModel", t(0.7F, 0.65F), t(0.45F, 1.8F)),
            eb("HeronModel", t(0.8F, 0.35F), t(0.4F, 2.05F)),
            eb("HummingbirdModel", t(0.3F, 4.0F), t(0.15F, 9.3F)),
            eb("KingfisherModel", null, t(0.4F, 2.2F)),
            eb("KiwiModel", null, t(0.7F, 0.6F)),
            eb("KookaburraModel", null, t(0.6F, 1.0F)),
            eb("LyrebirdModel", null, t(0.7F, 0.6F)),
            eb("MacawModel", t(0.95F, 0.09F), t(0.5F, 1.5F)),
            eb("MagpieModel", null, t(0.6F, 1.0F)),
            eb("OstrichModel", t(1.3F, -0.35F), t(0.7F, 0.6F)),
            eb("OwlModel", t(0.8F, 0.35F), t(0.5F, 1.45F)),
            eb("PeafowlModel", t(0.85F, 1.1F), t(0.45F, 2.7F)),
            eb("PelicanModel", t(1.1F, -0.18F), t(0.7F, 0.6F)),
            eb("PenguinModel", null, t(0.5F, 1.5F)),
            eb("PhoenixModel", t(1.3F, -0.3F), t(0.7F, 0.6F)),
            eb("PigeonModel", t(0.8F, 0.34F), t(0.4F, 2.2F)),
            eb("RoadrunnerModel", t(0.75F, 0.45F), t(0.45F, 1.8F)),
            eb("SongbirdModel", null, t(0.6F, 1.0F)),
            eb("SwanModel", t(1.0F, 0.0F, -0.2F), t(0.6F, 1.0F, -0.2F)),
            eb("ToucanModel", null, t(0.55F, 1.2F)),
            eb("WoodpeckerModel", t(0.7F, 0.0F, -0.2F), t(0.45F, 1.2F, -0.1F))
    );

    private ThirdPartyModelWraps() {
    }

    private static Transform t(float scale, float ty) {
        return new Transform(scale, 0.0F, ty, 0.0F);
    }

    private static Transform t(float scale, float ty, float tz) {
        return new Transform(scale, 0.0F, ty, tz);
    }

    private static Map.Entry<String, Entry> am(String modelClass, Transform young) {
        return Map.entry(ALEXS_MOBS_PKG + modelClass, new Entry(null, young));
    }

    private static Map.Entry<String, Entry> eb(String modelClass, @Nullable Transform adult, Transform baby) {
        return Map.entry(EXOTIC_BIRDS_PKG + modelClass, new Entry(adult, baby));
    }

    /**
     * Reproduce a listed model's whole-model render transform on {@code poseStack}, choosing the branch by
     * {@code model.young}. No-op for every unlisted model and for a listed model whose active branch
     * renders at identity.
     */
    public static void preTransform(EntityModel<?> model, PoseStack poseStack) {
        String name = model.getClass().getName();
        apply(EXOTIC_BIRDS.get(name), model.young, poseStack);
        apply(ALEXS_MOBS.get(name), model.young, poseStack);
    }

    private static void apply(@Nullable Entry entry, boolean young, PoseStack poseStack) {
        if (entry == null) {
            return;
        }
        Transform transform = young ? entry.baby() : entry.adult();
        if (transform != null) {
            transform.apply(poseStack);
        }
    }
}
