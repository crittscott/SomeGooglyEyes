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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

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
    //
    // Every field of every codec below is required. A config has exactly one shape on disk — nothing
    // reads "field absent" as different from "field equals its default" — and required fields are what
    // make `encode` write the complete form. With optionalFieldOf, encode elides any value equal to its
    // default, which would leave sparse files whose meaning silently tracks whatever the code defaults
    // later become. These files are authored by the picker and hand-edited afterwards, so they pin
    // every value explicitly.

    // Raw datapack file structure (one file per entity).
    public static class ConfigFile {
        public static final Codec<ConfigFile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                VersionedEntry.CODEC.listOf().fieldOf("entries").forGetter(f -> f.entries)
        ).apply(inst, entries -> {
            ConfigFile f = new ConfigFile();
            f.entries = entries;
            return f;
        }));

        public List<VersionedEntry> entries = List.of();

        /** A one-entry file for a single config, as {@code /sg export} writes it. */
        public static ConfigFile single(String versionRange, String age, RuntimeConfig config) {
            ConfigFile file = new ConfigFile();
            file.entries = List.of(VersionedEntry.of(versionRange, age, config));
            return file;
        }

        /**
         * A whole age-set as a multi-entry file, one entry per age that has a usable config after
         * pruning, or {@code null} when none does. Each head's attach token is mapped through
         * {@code canon} (its canonical form for the entity's model).
         */
        @Nullable
        public static ConfigFile ofSet(RuntimeConfigSet set, String versionRange, UnaryOperator<String> canon) {
            List<VersionedEntry> entries = new ArrayList<>();
            addAge(entries, "adult", set.adult, versionRange, canon);
            addAge(entries, "baby", set.baby, versionRange, canon);
            addAge(entries, "any", set.any, versionRange, canon);
            if (entries.isEmpty()) {
                return null;
            }
            ConfigFile file = new ConfigFile();
            file.entries = entries;
            return file;
        }

        private static void addAge(List<VersionedEntry> entries, String age, @Nullable RuntimeConfig config,
                                   String versionRange, UnaryOperator<String> canon) {
            RuntimeConfig pruned = RuntimeConfig.pruned(config, canon);
            if (pruned != null) {
                entries.add(VersionedEntry.of(versionRange, age, pruned));
            }
        }
    }

    public static class HeadConfig {
        public static final Codec<HeadConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("attachPoint").forGetter(h -> h.attachPoint),
                EyeDefinition.CODEC.listOf().fieldOf("eyes").forGetter(h -> h.eyes)
        ).apply(inst, (attachPoint, eyes) -> {
            HeadConfig h = new HeadConfig();
            h.attachPoint = attachPoint;
            h.eyes = eyes;
            return h;
        }));

        public String attachPoint;
        public List<EyeDefinition> eyes = List.of();
    }

    // Runtime structure selected by version and age, then synced to clients.
    public static class RuntimeConfig {
        public static final Codec<RuntimeConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.fieldOf("enabled").forGetter(c -> c.enabled),
                Variant.CODEC.listOf().fieldOf("variants").forGetter(c -> c.variants)
        ).apply(inst, (enabled, variants) -> {
            RuntimeConfig c = new RuntimeConfig();
            c.enabled = enabled;
            c.variants = variants;
            return c;
        }));

        public boolean enabled = true;
        // One or more placement variants; a mob picks one (weighted) at spawn. The loader keeps only
        // entries with at least one usable variant (see EyeConfigReloadListener#usableVariants).
        public List<Variant> variants = List.of();

        /**
         * Whether {@code config} can actually put eyes on a mob: present, enabled, and with at least
         * one variant. The single eligibility predicate shared by the server's slimy-eye targeting and
         * spawn gating ({@code ServerEyeConfigs}) and the client's held-eye inspect indicator
         * ({@code EyeInspectIndicator}), so the indicator can never disagree with what an application does.
         */
        public static boolean isUsable(@Nullable RuntimeConfig config) {
            return config != null && config.enabled && !config.variants.isEmpty();
        }

        /**
         * A copy with structurally-empty parts dropped (heads with no eyes, then variants with no
         * heads) and every attach token mapped through {@code canon}, or {@code null} when nothing
         * usable remains. The one place both export paths agree on what's worth writing.
         */
        @Nullable
        public static RuntimeConfig pruned(@Nullable RuntimeConfig config, UnaryOperator<String> canon) {
            if (config == null) {
                return null;
            }
            List<Variant> variants = new ArrayList<>();
            for (Variant v : config.variants) {
                List<HeadConfig> heads = new ArrayList<>();
                for (HeadConfig h : v.heads) {
                    if (h.eyes.isEmpty()) {
                        continue;
                    }
                    HeadConfig head = new HeadConfig();
                    head.attachPoint = canon.apply(h.attachPoint);
                    head.eyes = h.eyes;
                    heads.add(head);
                }
                if (heads.isEmpty()) {
                    continue; // skip arrangements with no usable eyes
                }
                Variant variant = new Variant();
                variant.weight = v.weight();
                variant.heads = heads;
                variants.add(variant);
            }
            if (variants.isEmpty()) {
                return null;
            }
            RuntimeConfig pruned = new RuntimeConfig();
            pruned.enabled = config.enabled;
            pruned.variants = variants;
            return pruned;
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
                Codec.DOUBLE.fieldOf("weight").forGetter(v -> v.weight),
                HeadConfig.CODEC.listOf().fieldOf("heads").forGetter(v -> v.heads)
        ).apply(inst, (weight, heads) -> {
            Variant v = new Variant();
            v.weight = weight;
            v.heads = heads;
            return v;
        }));

        public List<HeadConfig> heads = List.of();
        public double weight = 1.0; // relative probability

        /** Negative weights are clamped to 0. */
        public double weight() {
            return Math.max(0.0, weight);
        }
    }

    // One selectable entry in a datapack file.
    public static class VersionedEntry {
        public static final Codec<VersionedEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("version").forGetter(e -> e.version),
                Codec.STRING.fieldOf("age").forGetter(e -> e.age),
                Codec.BOOL.fieldOf("enabled").forGetter(e -> e.enabled),
                Variant.CODEC.listOf().fieldOf("variants").forGetter(e -> e.variants)
        ).apply(inst, (version, age, enabled, variants) -> {
            VersionedEntry e = new VersionedEntry();
            e.version = version;
            e.age = age;
            e.enabled = enabled;
            e.variants = variants;
            return e;
        }));

        public String age = "any";
        public boolean enabled = true;
        // Weighted placement variants; a mob picks one at spawn. The only placement shape on disk.
        public List<Variant> variants = List.of();
        public String version = "";

        /** One entry declaring {@code config} for a version range and age. */
        public static VersionedEntry of(String versionRange, String age, RuntimeConfig config) {
            VersionedEntry entry = new VersionedEntry();
            entry.version = versionRange;
            entry.age = age;
            entry.enabled = config.enabled;
            entry.variants = config.variants;
            return entry;
        }
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
     * Project a head-frame direction {@code (dx, dy, dz)} into an eye's pupil-plane {@code (right, up)}
     * basis — the same basis {@link #applyRotation} rotates the pupil into for the given
     * {@code inclination}/{@code azimuth}. Returns {@code {x, y}} (the look-axis component is dropped,
     * since the pupil moves in that plane). Cross-eye uses this to aim one pupil at another eye: it feeds
     * in the head-frame vector from this eye to its target and gets back the direction in pupil space.
     *
     * <p>Mirrors {@code applyRotation}'s rotation R = Ry(-(azimuth+90°)) · Rx(90°-inclination): the eye's
     * local right axis maps to {@code (cos a, 0, -sin a)} and up to {@code (sin a sin b, cos b, cos a sin b)}
     * with {@code a = -(azimuth+90°)}, {@code b = 90°-inclination}; the projection is the dot with each.
     */
    public static float[] projectToPupilPlane(double inclination, double azimuth, double dx, double dy, double dz) {
        double a = Math.toRadians(-(azimuth + 90.0));
        double b = Math.toRadians(90.0 - inclination);
        double ca = Math.cos(a);
        double sa = Math.sin(a);
        double cb = Math.cos(b);
        double sb = Math.sin(b);
        // right = R·(1,0,0), up = R·(0,1,0); pupil-plane coords are the dot of the direction with each.
        float x = (float) (dx * ca + dz * -sa);
        float y = (float) (dx * (sa * sb) + dy * cb + dz * (ca * sb));
        return new float[]{x, y};
    }

    /**
     * Resolve a mob's stored roll (0..1) to a variant index via cumulative weights. A {@code null}/empty
     * config yields 0. Deterministic: the same roll + config always picks the same variant, so server
     * and client agree without sending the resolved index.
     */
    public static int chooseVariantIndex(RuntimeConfig config, float roll) {
        if (config == null || config.variants.isEmpty()) {
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
        if (head == null || eyeIndex < 0 || eyeIndex >= head.eyes.size()) {
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
        return head != null ? head.eyes.size() : 0;
    }

    public float getEyeScale(int headIndex, int eyeIndex) {
        return placementAt(headIndex, eyeIndex).eyeScale();
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
        return entityConfig != null && entityConfig.enabled && heads != null && !heads.isEmpty();
    }

    private HeadConfig headAt(int headIndex) {
        if (!hasConfig() || headIndex < 0 || headIndex >= heads.size()) {
            return null;
        }
        return heads.get(headIndex);
    }

    private static List<HeadConfig> headsOfVariant(RuntimeConfig config, int variantIndex) {
        if (config == null || config.variants.isEmpty()) {
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
