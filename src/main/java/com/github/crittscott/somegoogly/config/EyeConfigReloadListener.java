package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.head.HeadInfo.ConfigFile;
import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.head.HeadInfo.VersionedEntry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads eye geometry configs from datapacks: scans {@code data/<namespace>/eyes/*.json}, one file
 * per entity (the file path is the entity id). Each file contains versioned/age-selected entries;
 * reload selects the entries that match the currently loaded mod version for that namespace.
 */
public class EyeConfigReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public EyeConfigReloadListener() {
        super(GSON, "eyes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, RuntimeConfigSet> selected = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                RuntimeConfigSet config = selectForLoadedVersion(entry.getKey(), GSON.fromJson(entry.getValue(), ConfigFile.class));
                if (config != null && config.hasAnyConfig()) {
                    selected.put(entry.getKey(), config);
                }
            } catch (Exception e) {
                SomeGoogly.LOGGER.error("Failed to parse eye config {}", entry.getKey(), e);
            }
        }
        ServerEyeConfigs.replaceAll(selected);
        SomeGoogly.LOGGER.info("Loaded {} selected eye configs from {} files", selected.size(), files.size());
    }

    private static RuntimeConfigSet selectForLoadedVersion(ResourceLocation entityId, ConfigFile file) {
        if (file == null || file.entries == null || file.entries.isEmpty()) {
            return null;
        }

        Optional<String> loadedVersion = ModVersionLookup.versionForNamespace(entityId.getNamespace());
        if (loadedVersion.isEmpty()) {
            return null;
        }

        RuntimeConfigSet set = new RuntimeConfigSet();
        for (VersionedEntry entry : file.entries) {
            if (entry == null || !VersionRangeMatcher.matches(entry.version, loadedVersion.get())) {
                continue;
            }

            String age = entry.age == null ? "" : entry.age.trim().toLowerCase(java.util.Locale.ROOT);
            RuntimeConfig runtime = toRuntime(entry);
            switch (age) {
                case "adult" -> set.adult = choose(entityId, "adult", set.adult, runtime);
                case "baby" -> set.baby = choose(entityId, "baby", set.baby, runtime);
                case "any" -> set.any = choose(entityId, "any", set.any, runtime);
                default -> SomeGoogly.LOGGER.warn("Ignoring eye config {} with invalid age '{}'", entityId, entry.age);
            }
        }
        return set;
    }

    private static RuntimeConfig choose(ResourceLocation entityId, String age, RuntimeConfig existing, RuntimeConfig next) {
        if (existing != null) {
            SomeGoogly.LOGGER.warn("Multiple SomeGoogly entries match {} age {}; keeping the first", entityId, age);
            return existing;
        }
        return next;
    }

    private static RuntimeConfig toRuntime(VersionedEntry entry) {
        RuntimeConfig runtime = new RuntimeConfig();
        runtime.enabled = entry.enabled;
        runtime.heads = entry.heads;
        return runtime;
    }
}
