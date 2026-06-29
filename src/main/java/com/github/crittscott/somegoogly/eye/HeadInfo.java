package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-entity, per-age eye configuration, as seen by the client renderer.
 *
 * <p>This is pure data: it answers "how many heads, which attachment part, and what eyes" for an
 * entity. Part resolution and positioning is done by the
 * {@link EyeAttachmentResolver}s, by string name.
 *
 * <p>Configs are loaded from datapacks on the server, selected by mod version + age, and synced to
 * the client; this class reads the client-side selected copy ({@link ClientEyeConfigs}). The
 * {@code attachPoint} in config is the string part token handed to the resolver.
 *
 * <p>Each eye is an {@link EyeDefinition} ({@link EyePlacement} + {@link EyeAppearance}); the whole
 * config tree (file → entries → variants → heads → eyes) serializes through the {@link Codec}s on the
 * nested classes here, shared by the datapack loader, the sync packet, and the picker exporter.
 */
public class HeadInfo {
    public static final double DEFAULT_AZIMUTH = EyePlacement.DEFAULT_AZIMUTH;
    /** Re-exported from {@link EyePlacement} so existing callers (picker) keep their reference. */
    public static final double DEFAULT_INCLINATION = EyePlacement.DEFAULT_INCLINATION;
    private static final Map<CacheKey, HeadInfo> headInfoCache = new HashMap<>();

    private final RuntimeConfig entityConfig;
    // The single placement variant chosen for this mob (by its stored roll). Null when no usable config.
    private final List<HeadConfig> heads;

    private HeadInfo(RuntimeConfig config, int variantIndex) {
        this.entityConfig = config;
        this.heads = headsOfVariant(config, variantIndex);
    }

    private record CacheKey(ResourceLocation entityName, boolean baby, int variant) {
    }

    // --- Datapack structure (mutable POJOs, each with a Codec; the file path supplies the entity id) ---

    // Raw datapack file structure (one file per entity).
    public static class ConfigFile {
        public static final Codec<ConfigFile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                VersionedEntry.CODEC.listOf().optionalFieldOf("entries", List.of())
                        .forGetter(f -> f.entries != null ? f.entries : List.of())
        ).apply(inst, entries -> {
            ConfigFile f = new ConfigFile();
            f.entries = entries;
            return f;
        }));

        public List<VersionedEntry> entries;
    }

    public static class HeadConfig {
        public static final Codec<HeadConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("attachPoint").forGetter(h -> h.attachPoint),
                EyeDefinition.CODEC.listOf().optionalFieldOf("eyes", List.of())
                        .forGetter(h -> h.eyes != null ? h.eyes : List.of())
        ).apply(inst, (attachPoint, eyes) -> {
            HeadConfig h = new HeadConfig();
            h.attachPoint = attachPoint;
            h.eyes = eyes;
            return h;
        }));

        public String attachPoint;
        public List<EyeDefinition> eyes;
    }

    // Runtime structure selected by version and age, then synced to clients.
    public static class RuntimeConfig {
        public static final Codec<RuntimeConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.optionalFieldOf("enabled").forGetter(c -> Optional.ofNullable(c.enabled)),
                Variant.CODEC.listOf().optionalFieldOf("variants", List.of())
                        .forGetter(c -> c.variants != null ? c.variants : List.of())
        ).apply(inst, (enabled, variants) -> {
            RuntimeConfig c = new RuntimeConfig();
            c.enabled = enabled.orElse(null);
            c.variants = variants;
            return c;
        }));

        public Boolean enabled;
        // One or more placement variants; a mob picks one (weighted) at spawn. The loader keeps only
        // entries with at least one usable variant (see EyeConfigReloadListener#usableVariants).
        public List<Variant> variants;

        /** Defaults to enabled when the field is absent. */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }
    }

    public static class RuntimeConfigSet {
        public static final Codec<RuntimeConfigSet> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                RuntimeConfig.CODEC.optionalFieldOf("adult").forGetter(s -> Optional.ofNullable(s.adult)),
                RuntimeConfig.CODEC.optionalFieldOf("baby").forGetter(s -> Optional.ofNullable(s.baby)),
                RuntimeConfig.CODEC.optionalFieldOf("any").forGetter(s -> Optional.ofNullable(s.any))
        ).apply(inst, (adult, baby, any) -> {
            RuntimeConfigSet s = new RuntimeConfigSet();
            s.adult = adult.orElse(null);
            s.baby = baby.orElse(null);
            s.any = any.orElse(null);
            return s;
        }));

        public RuntimeConfig adult;
        public RuntimeConfig any;
        public RuntimeConfig baby;

        public RuntimeConfig get(boolean isBaby) {
            RuntimeConfig ageConfig = isBaby ? baby : adult;
            return ageConfig != null ? ageConfig : any;
        }

        public boolean hasAnyConfig() {
            return adult != null || baby != null || any != null;
        }
    }

    /** One weighted placement arrangement: a complete set of heads (each with its own eyes). */
    public static class Variant {
        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.DOUBLE.optionalFieldOf("weight").forGetter(v -> Optional.ofNullable(v.weight)),
                HeadConfig.CODEC.listOf().optionalFieldOf("heads", List.of())
                        .forGetter(v -> v.heads != null ? v.heads : List.of())
        ).apply(inst, (weight, heads) -> {
            Variant v = new Variant();
            v.weight = weight.orElse(null);
            v.heads = heads;
            return v;
        }));

        public List<HeadConfig> heads;
        public Double weight; // relative probability; null = 1.0

        /** Negative weights are clamped to 0; absent defaults to 1. */
        public double weight() {
            return weight == null ? 1.0 : Math.max(0.0, weight);
        }
    }

    // One selectable entry in a datapack file.
    public static class VersionedEntry {
        public static final Codec<VersionedEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("version", "").forGetter(e -> e.version != null ? e.version : ""),
                Codec.STRING.optionalFieldOf("age", "any").forGetter(e -> e.age != null ? e.age : "any"),
                Codec.BOOL.optionalFieldOf("enabled").forGetter(e -> Optional.ofNullable(e.enabled)),
                Variant.CODEC.listOf().optionalFieldOf("variants").forGetter(e -> Optional.ofNullable(e.variants))
        ).apply(inst, (version, age, enabled, variants) -> {
            VersionedEntry e = new VersionedEntry();
            e.version = version;
            e.age = age;
            e.enabled = enabled.orElse(null);
            e.variants = variants.orElse(null);
            return e;
        }));

        public String age;
        public Boolean enabled;
        // Weighted placement variants; a mob picks one at spawn. The only placement shape on disk.
        public List<Variant> variants;
        public String version;

        /** Defaults to enabled when the field is absent. */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }
    }

    public boolean affectedByInvisibility(int headIndex, int eyeIndex) {
        return placementAt(headIndex, eyeIndex).affectedByInvisibility();
    }

    /** The eye's config appearance (color/glow), or {@link EyeAppearance#DEFAULT} when out of range. */
    public EyeAppearance appearanceAt(int headIndex, int eyeIndex) {
        EyeDefinition eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? eye.appearance() : EyeAppearance.DEFAULT;
    }

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

    /** Apply {@link #applyRotation(PoseStack, double, double)} using an eye placement's angles. */
    public static void applyRotation(PoseStack poseStack, EyePlacement placement) {
        applyRotation(poseStack, placement.inclination(), placement.azimuth());
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

    /** Drop cached helpers; called when the client receives new configs or disconnects. */
    public static void clearCache() {
        headInfoCache.clear();
    }

    private EyeDefinition eyeAt(int headIndex, int eyeIndex) {
        HeadConfig head = headAt(headIndex);
        if (head == null || head.eyes == null || eyeIndex < 0 || eyeIndex >= head.eyes.size()) {
            return null;
        }
        return head.eyes.get(eyeIndex);
    }

    /** The part token (string name) the resolver should attach this head's eyes to. */
    public String getAttachToken(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null ? head.attachPoint : null;
    }

    public int getEyeCount(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null && head.eyes != null ? head.eyes.size() : 0;
    }

    public float getEyeScale(int headIndex, int eyeIndex) {
        return (float) placementAt(headIndex, eyeIndex).eyeScale();
    }

    public int getHeadCount() {
        return hasConfig() ? heads.size() : 0;
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

    /** Whether this entity has a usable, enabled placement variant. */
    public boolean hasConfig() {
        return entityConfig != null && entityConfig.isEnabled() && heads != null && !heads.isEmpty();
    }

    private HeadConfig headAt(int headIndex) {
        if (!hasConfig() || headIndex < 0 || headIndex >= heads.size()) {
            return null;
        }
        return heads.get(headIndex);
    }

    private static List<HeadConfig> headsOfVariant(RuntimeConfig config, int variantIndex) {
        if (config == null || config.variants == null || config.variants.isEmpty()) {
            return null;
        }
        int clamped = Math.max(0, Math.min(variantIndex, config.variants.size() - 1));
        return config.variants.get(clamped).heads;
    }

    /** The selected variant's head list (identity used to invalidate per-mob trackers). */
    public List<HeadConfig> headsRef() {
        return heads;
    }

    /** The eye's placement (geometry), or {@link EyePlacement#DEFAULT} when out of range. */
    public EyePlacement placementAt(int headIndex, int eyeIndex) {
        EyeDefinition eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? eye.placement() : EyePlacement.DEFAULT;
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
}
