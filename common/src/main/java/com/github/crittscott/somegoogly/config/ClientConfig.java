package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client-local rendering preferences shared across loaders. */
public final class ClientConfig {

    public static final String DISABLE_GOOGLY_EYES_KEY = "disableGooglyEyes";
    public static final boolean DISABLE_GOOGLY_EYES_DEFAULT = false;
    public static final String DISABLED_ENTITIES_KEY = "disabledEntities";
    public static final List<String> DISABLED_ENTITIES_DEFAULT = List.of();
    public static final String DISABLED_MODS_KEY = "disabledMods";
    public static final List<String> DISABLED_MODS_DEFAULT = List.of();

    public static final ConfigValue<Boolean> DISABLE_GOOGLY_EYES = ConfigValue.bool(DISABLE_GOOGLY_EYES_DEFAULT);
    public static final ConfigValue<List<String>> DISABLED_ENTITIES =
            ConfigValue.strings(DISABLED_ENTITIES_DEFAULT, value -> true);
    public static final ConfigValue<List<String>> DISABLED_MODS =
            ConfigValue.strings(DISABLED_MODS_DEFAULT, value -> true);

    private static final Set<String> loggedBadDisabledEntityEntries = new HashSet<>();
    private static Set<ResourceLocation> cachedDisabledEntityIds;
    private static Set<String> cachedDisabledMods;

    private ClientConfig() {
    }

    public static Set<ResourceLocation> disabledEntityIds() {
        Set<ResourceLocation> cached = cachedDisabledEntityIds;
        if (cached == null) {
            cached = parseEntityIds(DISABLED_ENTITIES.get());
            cachedDisabledEntityIds = cached;
        }
        return cached;
    }

    private static Set<String> disabledMods() {
        Set<String> cached = cachedDisabledMods;
        if (cached == null) {
            cached = new LinkedHashSet<>();
            for (String entry : DISABLED_MODS.get()) {
                if (entry != null && !entry.trim().isEmpty()) {
                    cached.add(entry.trim());
                }
            }
            cachedDisabledMods = cached;
        }
        return cached;
    }

    public static void invalidateCaches() {
        cachedDisabledEntityIds = null;
        cachedDisabledMods = null;
    }

    public static boolean isEntityDisabled(ResourceLocation entityType) {
        return disabledMods().contains(entityType.getNamespace()) || disabledEntityIds().contains(entityType);
    }

    /** Restore built-in defaults before a loader applies values from disk. */
    public static void resetDefaults() {
        DISABLE_GOOGLY_EYES.reset();
        DISABLED_ENTITIES.reset();
        DISABLED_MODS.reset();
        invalidateCaches();
    }

    private static void logBadDisabledEntityEntry(String entry) {
        String key = String.valueOf(entry);
        if (loggedBadDisabledEntityEntries.add(key)) {
            SomeGooglyCommon.LOGGER.warn(
                    "Dropping invalid client disabledEntities entry '{}'; expected an entity id like 'minecraft:zombie'",
                    key);
        }
    }

    private static ResourceLocation parseDisabledEntityId(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            logBadDisabledEntityEntry(entry);
            return null;
        }
        try {
            return ResourceLocation.parse(entry.trim());
        } catch (Exception e) {
            logBadDisabledEntityEntry(entry);
            return null;
        }
    }

    private static Set<ResourceLocation> parseEntityIds(List<String> entries) {
        Set<ResourceLocation> parsed = new LinkedHashSet<>();
        for (String entry : entries) {
            ResourceLocation id = parseDisabledEntityId(entry);
            if (id != null) {
                parsed.add(id);
            }
        }
        return parsed;
    }
}
