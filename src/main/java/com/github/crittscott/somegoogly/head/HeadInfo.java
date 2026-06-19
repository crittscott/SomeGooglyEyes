package com.github.crittscott.somegoogly.head;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.state.EyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-entity, per-age eye configuration, as seen by the client renderer.
 *
 * <p>This is pure data: it answers "how many heads, which attachment part, and what eyes" for an
 * entity. Part resolution and positioning is done by the
 * {@link com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver}s, by string name.
 *
 * <p>Configs are loaded from datapacks on the server, selected by mod version + age, and synced to
 * the client; this class reads the client-side selected copy ({@link ClientEyeConfigs}). The
 * {@code attachPoint} in config is the string part token handed to the resolver.
 */
public class HeadInfo {
    private static final Map<CacheKey, HeadInfo> headInfoCache = new HashMap<>();

    private final RuntimeConfig entityConfig;
    // The single placement variant chosen for this mob (by its stored roll). Null when no usable config.
    private final List<HeadConfig> heads;

    private HeadInfo(RuntimeConfig config, int variantIndex) {
        this.entityConfig = config;
        this.heads = headsOfVariant(config, variantIndex);
    }

    /**
     * Client-side helper for rendering: reads the synced {@link ClientEyeConfigs}, resolves the mob's
     * placement variant from its stored roll, and caches per (entity, age, variant) so same-arrangement
     * mobs share one instance.
     */
    public static HeadInfo getHelper(ResourceLocation entityName, LivingEntity entity) {
        boolean baby = entity.isBaby();
        RuntimeConfig config = ClientEyeConfigs.get(entityName, baby);
        int variant = chooseVariantIndex(config, EyeState.getVariantRoll(entity));
        return headInfoCache.computeIfAbsent(new CacheKey(entityName, baby, variant),
                key -> new HeadInfo(config, key.variant));
    }

    /**
     * Server-side helper (harvest / kill-drop): reads the authoritative {@link ServerEyeConfigs} rather
     * than the client store, so geometry is correct on a dedicated server. Not cached — server use is
     * infrequent and must not share the client cache (single-player runs both stores in one JVM).
     */
    public static HeadInfo serverHelper(ResourceLocation entityName, LivingEntity entity) {
        RuntimeConfig config = ServerEyeConfigs.get(entityName, entity);
        int variant = chooseVariantIndex(config, EyeState.getVariantRoll(entity));
        return new HeadInfo(config, variant);
    }

    /** Drop cached helpers; called when the client receives new configs or disconnects. */
    public static void clearCache() {
        headInfoCache.clear();
    }

    /** Whether this entity has a usable, enabled placement variant. */
    public boolean hasConfig() {
        return entityConfig != null && entityConfig.isEnabled() && heads != null && !heads.isEmpty();
    }

    public int getHeadCount() {
        return hasConfig() ? heads.size() : 0;
    }

    /** The part token (string name) the resolver should attach this head's eyes to. */
    public String getAttachToken(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null ? head.attachPoint : "head";
    }

    public int getEyeCount(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null && head.eyes != null ? head.eyes.size() : 0;
    }

    public boolean doesEyeGlow(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null && eye.glows;
    }

    public boolean affectedByInvisibility(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye == null || eye.affectedByInvisibility;
    }

    public float getEyeScale(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.eyeScale : 0.75f;
    }

    public float getIrisScale(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.irisScale : 0.6f;
    }

    public float getEyeSideOffset(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.sideOffset : 0.0f;
    }

    public double getInclination(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null && eye.inclination != null ? eye.inclination : DEFAULT_INCLINATION;
    }

    public double getAzimuth(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null && eye.azimuth != null ? eye.azimuth : DEFAULT_AZIMUTH;
    }

    /** Default orientation: pupil facing local -Z (straight ahead), matching the unrotated eye. */
    public static final double DEFAULT_INCLINATION = 90.0;
    public static final double DEFAULT_AZIMUTH = 270.0;

    /**
     * Aim the eye via two angles instead of a quaternion: {@code inclination} measured from the part's
     * +Y axis and {@code azimuth} from its +X axis (both degrees). The eye's pupil faces local -Z by
     * default; this rotates that axis onto the direction {@code (sinθcosφ, cosθ, sinθsinφ)}. Roll is
     * irrelevant (the eye is rotationally symmetric about its look axis), so two angles suffice.
     */
    public static void applyRotation(PoseStack poseStack, double inclination, double azimuth) {
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-(azimuth + 90.0))));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90.0 - inclination)));
    }

    /** Apply {@link #applyRotation(PoseStack, double, double)} using an eye's angles (null = default). */
    public static void applyRotation(PoseStack poseStack, EyeConfig eye) {
        double inc = eye != null && eye.inclination != null ? eye.inclination : DEFAULT_INCLINATION;
        double azi = eye != null && eye.azimuth != null ? eye.azimuth : DEFAULT_AZIMUTH;
        applyRotation(poseStack, inc, azi);
    }

    public float[] getEyeOffsetFromJoint(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        if (eye == null || eye.position == null || eye.position.length < 3) {
            return new float[]{eyeIndex == 0 ? -0.13f : 0.13f, -0.25f, -0.25f};
        }
        return new float[]{(float) eye.position[0], (float) eye.position[1], (float) eye.position[2]};
    }

    public float[] getCorneaColours(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        if (eye == null || eye.corneaColors == null || eye.corneaColors.length < 3) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        return new float[]{(float) eye.corneaColors[0], (float) eye.corneaColors[1], (float) eye.corneaColors[2]};
    }

    public float[] getIrisColours(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        if (eye == null || eye.irisColors == null || eye.irisColors.length < 3) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        return new float[]{(float) eye.irisColors[0], (float) eye.irisColors[1], (float) eye.irisColors[2]};
    }

    private HeadConfig headAt(int headIndex) {
        if (!hasConfig() || headIndex < 0 || headIndex >= heads.size()) {
            return null;
        }
        return heads.get(headIndex);
    }

    /** The selected variant's head list (identity used to invalidate per-mob trackers). */
    public List<HeadConfig> headsRef() {
        return heads;
    }

    /**
     * Resolve a mob's stored roll (0..1) to a variant index via cumulative weights. A {@code null}/empty
     * config yields 0. Deterministic: the same roll + config always picks the same variant, so server
     * and client agree without sending the resolved index.
     */
    public static int chooseVariantIndex(RuntimeConfig config, float roll) {
        if (config == null || config.variants == null || config.variants.isEmpty()) {
            return 0;
        }
        List<Variant> variants = config.variants;
        double total = 0;
        for (Variant v : variants) {
            total += v.weight();
        }
        if (total <= 0) {
            return 0;
        }
        double target = roll * total;
        double acc = 0;
        for (int i = 0; i < variants.size(); i++) {
            acc += variants.get(i).weight();
            if (target < acc) {
                return i;
            }
        }
        return variants.size() - 1;
    }

    private static List<HeadConfig> headsOfVariant(RuntimeConfig config, int variantIndex) {
        if (config == null || config.variants == null || config.variants.isEmpty()) {
            return null;
        }
        int clamped = Math.max(0, Math.min(variantIndex, config.variants.size() - 1));
        return config.variants.get(clamped).heads;
    }

    private EyeConfig eyeAt(int headIndex, int eyeIndex) {
        HeadConfig head = headAt(headIndex);
        if (head == null || head.eyes == null || eyeIndex < 0 || eyeIndex >= head.eyes.size()) {
            return null;
        }
        return head.eyes.get(eyeIndex);
    }

    public RuntimeConfig config() {
        return entityConfig;
    }

    private record CacheKey(ResourceLocation entityName, boolean baby, int variant) {
    }

    // Raw datapack file structure (one file per entity; entity id comes from the file path).
    public static class ConfigFile {
        public List<VersionedEntry> entries;
    }

    // One selectable entry in a datapack file.
    public static class VersionedEntry {
        public String version;
        public String age;
        public Boolean enabled;
        // Legacy single arrangement; equivalent to a single weight-1 variant. Either this or `variants`.
        public List<HeadConfig> heads;
        // Weighted placement variants (a mob picks one). Takes precedence over `heads` when present.
        public List<Variant> variants;

        /** Defaults to enabled when the field is absent. */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }
    }

    // Runtime structure selected by version and age, then synced to clients.
    public static class RuntimeConfig {
        public Boolean enabled;
        // One or more placement variants; a mob picks one (weighted) at spawn. The loader always fills
        // this with at least one entry (a legacy bare `heads` becomes a single weight-1 variant).
        public List<Variant> variants;

        /** Defaults to enabled when the field is absent. */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }

        /** The first variant's heads (the picker authors/export a single arrangement). */
        public List<HeadConfig> primaryHeads() {
            return variants != null && !variants.isEmpty() ? variants.get(0).heads : null;
        }
    }

    /** One weighted placement arrangement: a complete set of heads (each with its own eyes). */
    public static class Variant {
        public Double weight; // relative probability; null = 1.0
        public List<HeadConfig> heads;

        /** Negative weights are clamped to 0; absent defaults to 1. */
        public double weight() {
            return weight == null ? 1.0 : Math.max(0.0, weight);
        }
    }

    public static class RuntimeConfigSet {
        public RuntimeConfig adult;
        public RuntimeConfig baby;
        public RuntimeConfig any;

        public RuntimeConfig get(boolean isBaby) {
            RuntimeConfig ageConfig = isBaby ? baby : adult;
            return ageConfig != null ? ageConfig : any;
        }

        public boolean hasAnyConfig() {
            return adult != null || baby != null || any != null;
        }
    }

    public static class HeadConfig {
        public String attachPoint;
        public List<EyeConfig> eyes;
    }

    public static class EyeConfig {
        public double[] position;
        public double eyeScale;
        public double irisScale;
        public double sideOffset;
        public Double inclination; // angle from part +Y (degrees); null = default forward
        public Double azimuth;     // angle from part +X (degrees); null = default forward
        public double[] corneaColors;
        public double[] irisColors;
        public boolean glows;
        public boolean affectedByInvisibility;
    }
}
