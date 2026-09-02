package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigModel.ConfigFile;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.EyeConfigModel.VersionedEntry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ANY;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

/**
 * Loads eye geometry configs from datapacks: scans {@code data/<namespace>/eyes/*.json}, one file
 * per entity (the file path is the entity id). Each file contains versioned/age-selected entries;
 * reload selects the entries that match the currently loaded mod version for that namespace. When
 * no entry matches, the nearest generation is used instead ({@link VersionRangeMatcher#nearestVersion})
 * and the mismatch is logged — eyes are cosmetic, so degraded placement beats silently dropping the
 * file (which would also permanently store a no-eyes roll for mobs spawned during the window).
 */
public class EyeConfigReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public EyeConfigReloadListener() {
        super(GSON, "eyes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, RuntimeConfigSet> selected = new HashMap<>();
        int skippedModNotInstalled = 0;
        int failedParse = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            // Hard exclusion, not config: no config for the dragon may ever load, from any datapack.
            if (entry.getKey().equals(ServerEyeConfigs.ENDER_DRAGON)) {
                SomeGooglyCommon.LOGGER.warn(
                        "Ignoring eye config {} — the ender dragon is hard-excluded from googly eyes", entry.getKey());
                continue;
            }
            // A bundled definition for an absent optional mod is the common case, not an error: skip it
            // quietly and account for it in the summary so the loaded/total gap doesn't read as data loss.
            if (ModVersionLookup.versionForNamespace(entry.getKey().getNamespace()).isEmpty()) {
                skippedModNotInstalled++;
                continue;
            }
            try {
                // Every field is required, so a typo'd datapack fails to parse rather than quietly
                // decoding to defaults — say which file and which field rather than dropping it silently.
                ConfigFile file = ConfigFile.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> SomeGooglyCommon.LOGGER.error(
                                "Invalid eye config {}: {}", entry.getKey(), error))
                        .orElse(null);
                if (file == null) {
                    failedParse++;
                    continue;
                }
                RuntimeConfigSet config = selectForLoadedVersion(entry.getKey(), file);
                if (config != null && config.hasAnyConfig()) {
                    String error = EyeConfigLimits.validateSync(Map.of(entry.getKey(), config));
                    if (error == null) {
                        selected.put(entry.getKey(), config);
                    } else {
                        SomeGooglyCommon.LOGGER.error("Ignoring unsafe eye config {}: {}", entry.getKey(), error);
                    }
                }
            } catch (Exception e) {
                failedParse++;
                SomeGooglyCommon.LOGGER.error("Failed to parse eye config {}", entry.getKey(), e);
            }
        }
        String aggregateError = EyeConfigLimits.validateSync(selected);
        if (aggregateError != null) {
            SomeGooglyCommon.LOGGER.error(
                    "Eye config reload exceeds synchronized work limits; keeping the previous configs: {}",
                    aggregateError);
            return;
        }
        ServerEyeConfigs.replaceAll(selected);
        SomeGooglyCommon.LOGGER.info(
                "Loaded {} selected eye configs from {} files ({} skipped: mod not installed, {} failed to parse)",
                selected.size(), files.size(), skippedModNotInstalled, failedParse);
    }

    private static RuntimeConfig choose(ResourceLocation entityId, String age, RuntimeConfig existing, RuntimeConfig next) {
        if (existing != null) {
            SomeGooglyCommon.LOGGER.warn("Multiple SomeGoogly entries match {} age {}; keeping the first", entityId, age);
            return existing;
        }
        return next;
    }

    private static RuntimeConfigSet selectForLoadedVersion(ResourceLocation entityId, ConfigFile file) {
        if (file == null || file.entries.isEmpty()) {
            return null;
        }

        Optional<String> loadedVersion = ModVersionLookup.versionForNamespace(entityId.getNamespace());
        if (loadedVersion.isEmpty()) {
            return null;
        }
        String loaded = loadedVersion.get();

        List<VersionedEntry> validEntries = new ArrayList<>();
        for (VersionedEntry entry : file.entries) {
            if (VersionRangeMatcher.isValid(entry.version)) {
                validEntries.add(entry);
            } else {
                SomeGooglyCommon.LOGGER.warn(
                        "Invalid eye config {} version range '{}'; entry will not match",
                        entityId, entry.version);
            }
        }

        RuntimeConfigSet set = select(entityId, validEntries,
                entry -> VersionRangeMatcher.matches(entry.version, loaded));
        if (set.hasAnyConfig()) {
            return set;
        }

        // No entry declares itself valid for the installed version. A misplaced eye can't crash
        // anything (unresolved attach tokens simply don't attach), so fall back to the nearest
        // generation instead of dropping the file — and say so in the log.
        List<String> declared = new ArrayList<>();
        for (VersionedEntry entry : validEntries) {
            declared.add(entry.version);
        }
        String nearest = VersionRangeMatcher.nearestVersion(declared, loaded);
        if (nearest == null) {
            return set; // nothing usable declared anywhere; nothing to fall back to
        }
        if (VersionRangeMatcher.isEntirelyBelow(nearest, loaded)) {
            SomeGooglyCommon.LOGGER.warn(
                    "Eye config {} has no entry for installed version {} of '{}'; using its newest entry"
                            + " (version {}). Placement may be slightly off until it is re-exported for the"
                            + " installed version — expected for bundled optional-mod definitions.",
                    entityId, loaded, entityId.getNamespace(), nearest);
        } else {
            SomeGooglyCommon.LOGGER.warn(
                    "Eye config {} has no entry for installed version {} of '{}'; using its oldest entry"
                            + " (version {}) — expected after a mod downgrade.",
                    entityId, loaded, entityId.getNamespace(), nearest);
        }
        // Same-version entries fall back as one generation, so adult/baby pairs stay together.
        return select(entityId, validEntries, entry -> nearest.equals(entry.version));
    }

    /** Run the age bucketing (adult/baby/any, first entry per bucket wins) over the accepted entries. */
    private static RuntimeConfigSet select(ResourceLocation entityId, List<VersionedEntry> entries,
                                           Predicate<VersionedEntry> versionFilter) {
        RuntimeConfigSet set = new RuntimeConfigSet();
        for (VersionedEntry entry : entries) {
            if (!versionFilter.test(entry)) {
                continue;
            }

            String age = entry.age.trim().toLowerCase(Locale.ROOT);
            RuntimeConfig runtime = toRuntime(entry);
            switch (age) {
                case AGE_ADULT -> set.adult = choose(entityId, AGE_ADULT, set.adult, runtime);
                case AGE_BABY -> set.baby = choose(entityId, AGE_BABY, set.baby, runtime);
                case AGE_ANY -> set.any = choose(entityId, AGE_ANY, set.any, runtime);
                default -> SomeGooglyCommon.LOGGER.warn(
                        "Ignoring eye config {} with invalid age '{}'", entityId, entry.age);
            }
        }
        return set;
    }

    private static RuntimeConfig toRuntime(VersionedEntry entry) {
        RuntimeConfig runtime = new RuntimeConfig();
        runtime.enabled = entry.enabled;
        runtime.variants = usableVariants(entry);
        return runtime;
    }

    /**
     * Filter an entry's {@code variants} down to the usable ones (those with at least one head).
     * Variants are the only placement shape on disk; a config left with none is not
     * {@link RuntimeConfig#isUsable}.
     */
    private static List<Variant> usableVariants(VersionedEntry entry) {
        List<Variant> result = new ArrayList<>();
        for (Variant v : entry.variants) {
            if (!v.heads.isEmpty()) {
                result.add(v);
            }
        }
        return result;
    }
}
