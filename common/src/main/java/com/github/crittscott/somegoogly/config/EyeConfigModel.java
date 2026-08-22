package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * The complete eye-definition data model shared by datapack loading, runtime selection, network
 * synchronization, and picker export. All serialized shapes live together here; resolved rendering
 * views and side-specific storage live elsewhere.
 */
public final class EyeConfigModel {

    public static final String AGE_ADULT = "adult";
    public static final String AGE_BABY = "baby";
    public static final String AGE_ANY = "any";

    private EyeConfigModel() {
    }

    /** Resolve a stable 0..1 roll through a runtime config's cumulative variant weights. */
    public static int chooseVariantIndex(@Nullable RuntimeConfig config, float roll) {
        if (config == null || config.variants.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (Variant variant : config.variants) {
            total += variant.weight();
        }
        if (total <= 0) {
            return 0;
        }
        double target = roll * total;
        double accumulated = 0;
        for (int i = 0; i < config.variants.size(); i++) {
            accumulated += config.variants.get(i).weight();
            if (target < accumulated) {
                return i;
            }
        }
        return config.variants.size() - 1;
    }

    /** Raw datapack file structure (one file per entity). */
    public static class ConfigFile {
        public static final Codec<ConfigFile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                VersionedEntry.CODEC.listOf().fieldOf("entries").forGetter(file -> file.entries)
        ).apply(inst, entries -> {
            ConfigFile file = new ConfigFile();
            file.entries = entries;
            return file;
        }));

        public List<VersionedEntry> entries = List.of();

        public static ConfigFile single(String versionRange, String age, RuntimeConfig config) {
            ConfigFile file = new ConfigFile();
            file.entries = List.of(VersionedEntry.of(versionRange, age, config));
            return file;
        }

        /** Build a pruned, canonicalized multi-age file, or {@code null} when no age is usable. */
        @Nullable
        public static ConfigFile ofSet(RuntimeConfigSet set, String versionRange, UnaryOperator<String> canon) {
            List<VersionedEntry> entries = new ArrayList<>();
            addAge(entries, AGE_ADULT, set.adult, versionRange, canon);
            addAge(entries, AGE_BABY, set.baby, versionRange, canon);
            addAge(entries, AGE_ANY, set.any, versionRange, canon);
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

    /** One head attachment and the eyes placed on it. */
    public static class HeadConfig {
        public static final Codec<HeadConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("attachPoint").forGetter(head -> head.attachPoint),
                EyeDefinition.CODEC.listOf().fieldOf("eyes").forGetter(head -> head.eyes)
        ).apply(inst, (attachPoint, eyes) -> {
            HeadConfig head = new HeadConfig();
            head.attachPoint = attachPoint;
            head.eyes = eyes;
            return head;
        }));

        public String attachPoint;
        public List<EyeDefinition> eyes = List.of();
    }

    /** Runtime structure selected by version and age, then synchronized to clients. */
    public static class RuntimeConfig {
        public static final Codec<RuntimeConfig> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.fieldOf("enabled").forGetter(config -> config.enabled),
                Variant.CODEC.listOf().fieldOf("variants").forGetter(config -> config.variants)
        ).apply(inst, (enabled, variants) -> {
            RuntimeConfig config = new RuntimeConfig();
            config.enabled = enabled;
            config.variants = variants;
            return config;
        }));

        public boolean enabled = true;
        public List<Variant> variants = List.of();

        public static boolean isUsable(@Nullable RuntimeConfig config) {
            return config != null && config.enabled && !config.variants.isEmpty();
        }

        /** Drop empty heads and variants while canonicalizing attachment tokens. */
        @Nullable
        public static RuntimeConfig pruned(@Nullable RuntimeConfig config, UnaryOperator<String> canon) {
            if (config == null) {
                return null;
            }
            List<Variant> variants = new ArrayList<>();
            for (Variant sourceVariant : config.variants) {
                List<HeadConfig> heads = new ArrayList<>();
                for (HeadConfig sourceHead : sourceVariant.heads) {
                    if (sourceHead.eyes.isEmpty()) {
                        continue;
                    }
                    HeadConfig head = new HeadConfig();
                    head.attachPoint = canon.apply(sourceHead.attachPoint);
                    head.eyes = sourceHead.eyes;
                    heads.add(head);
                }
                if (heads.isEmpty()) {
                    continue;
                }
                Variant variant = new Variant();
                variant.weight = sourceVariant.weight();
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

    /** The adult, baby, and age-independent runtime configs for one entity type. */
    public static class RuntimeConfigSet {
        public static final Codec<RuntimeConfigSet> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                RuntimeConfig.CODEC.optionalFieldOf(AGE_ADULT).forGetter(set -> Optional.ofNullable(set.adult)),
                RuntimeConfig.CODEC.optionalFieldOf(AGE_BABY).forGetter(set -> Optional.ofNullable(set.baby)),
                RuntimeConfig.CODEC.optionalFieldOf(AGE_ANY).forGetter(set -> Optional.ofNullable(set.any))
        ).apply(inst, (adult, baby, any) -> {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.adult = adult.orElse(null);
            set.baby = baby.orElse(null);
            set.any = any.orElse(null);
            return set;
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

    /** One weighted placement arrangement. */
    public static class Variant {
        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.DOUBLE.fieldOf("weight").forGetter(variant -> variant.weight),
                HeadConfig.CODEC.listOf().fieldOf("heads").forGetter(variant -> variant.heads)
        ).apply(inst, (weight, heads) -> {
            Variant variant = new Variant();
            variant.weight = weight;
            variant.heads = heads;
            return variant;
        }));

        public List<HeadConfig> heads = List.of();
        public double weight = 1.0;

        public double weight() {
            return Math.max(0.0, weight);
        }
    }

    /** One version- and age-selectable entry in a datapack file. */
    public static class VersionedEntry {
        public static final Codec<VersionedEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("version").forGetter(entry -> entry.version),
                Codec.STRING.fieldOf("age").forGetter(entry -> entry.age),
                Codec.BOOL.fieldOf("enabled").forGetter(entry -> entry.enabled),
                Variant.CODEC.listOf().fieldOf("variants").forGetter(entry -> entry.variants)
        ).apply(inst, (version, age, enabled, variants) -> {
            VersionedEntry entry = new VersionedEntry();
            entry.version = version;
            entry.age = age;
            entry.enabled = enabled;
            entry.variants = variants;
            return entry;
        }));

        public String age = AGE_ANY;
        public boolean enabled = true;
        public List<Variant> variants = List.of();
        public String version = "";

        public static VersionedEntry of(String versionRange, String age, RuntimeConfig config) {
            VersionedEntry entry = new VersionedEntry();
            entry.version = versionRange;
            entry.age = age;
            entry.enabled = config.enabled;
            entry.variants = config.variants;
            return entry;
        }
    }
}
