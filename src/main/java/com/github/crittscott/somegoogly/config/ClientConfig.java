package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DISABLE_GOOGLY_EYES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_MODS;
    private static final Set<String> loggedBadDisabledEntityEntries = new HashSet<>();

    // Parsed-and-cached views of the two list configs, rebuilt only when the config (re)loads rather than
    // on every render call. Guarded by needing a parse only after invalidation; access is single-threaded
    // (client render thread). See onConfigChanged().
    private static Set<ResourceLocation> cachedDisabledEntityIds;
    private static Set<String> cachedDisabledMods;

    static {
        BUILDER.push("Client Settings");

        DISABLE_GOOGLY_EYES = BUILDER
                .comment("Disable all googly eyes (client-side override)")
                .define("disableGooglyEyes", false);

        DISABLED_ENTITIES = BUILDER
                .comment("List of entity ids that should not get googly eyes, e.g. \"minecraft:zombie\" (client-side override)")
                .defineList("disabledEntities", ArrayList::new, obj -> obj instanceof String);

        DISABLED_MODS = BUILDER
                .comment("List of mod namespaces whose entities should not get googly eyes, e.g. \"minecraft\" (client-side override)")
                .defineList("disabledMods", ArrayList::new, obj -> obj instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
        // Drop the parsed caches whenever this config is (re)loaded or edited in-game, so disabledEntities /
        // disabledMods take effect without restart and without re-parsing on every render call.
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            invalidateCaches();
        }
    }

    /** Drop the parsed caches so the next access re-reads the (re)loaded config. */
    public static void invalidateCaches() {
        cachedDisabledEntityIds = null;
        cachedDisabledMods = null;
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

    public static boolean isEntityDisabled(ResourceLocation entityType) {
        return disabledMods().contains(entityType.getNamespace()) || disabledEntityIds().contains(entityType);
    }

    private static Set<ResourceLocation> parseEntityIds(List<? extends String> entries) {
        Set<ResourceLocation> parsed = new LinkedHashSet<>();
        for (String entry : entries) {
            ResourceLocation id = parseDisabledEntityId(entry);
            if (id != null) {
                parsed.add(id);
            }
        }
        return parsed;
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

    private static void logBadDisabledEntityEntry(String entry) {
        String key = String.valueOf(entry);
        if (loggedBadDisabledEntityEntries.add(key)) {
            SomeGoogly.LOGGER.error("Dropping invalid client disabledEntities entry '{}'; expected an entity id like 'minecraft:zombie'", key);
        }
    }
}
