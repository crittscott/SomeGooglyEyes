package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client-local rendering preferences shared by both loaders. */
public final class ClientConfig {

    public static final ConfigValue<Boolean> DISABLE_GOOGLY_EYES = ConfigValue.bool(false);
    public static final ConfigValue<List<String>> DISABLED_ENTITIES = ConfigValue.strings(List.of(), value -> true);
    public static final ConfigValue<List<String>> DISABLED_MODS = ConfigValue.strings(List.of(), value -> true);

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
            SomeGooglyCommon.LOGGER.error(
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
            return new ResourceLocation(entry.trim());
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
