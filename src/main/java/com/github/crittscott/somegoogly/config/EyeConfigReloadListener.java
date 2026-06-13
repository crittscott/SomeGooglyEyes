package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.head.HeadInfo.EntityConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads eye geometry configs from datapacks: scans {@code data/<namespace>/eyes/*.json}, one file
 * per entity (the file path is the entity id). Registered on {@code AddReloadListenerEvent}, so it
 * runs on server start and {@code /reload}, and the resource system has already resolved datapack
 * override/stacking per file. Results go to {@link ServerEyeConfigs}; sync to clients happens via
 * {@code OnDatapackSyncEvent}.
 */
public class EyeConfigReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public EyeConfigReloadListener() {
        super(GSON, "eyes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, EntityConfig> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                EntityConfig config = GSON.fromJson(entry.getValue(), EntityConfig.class);
                if (config != null) {
                    parsed.put(entry.getKey(), config);
                }
            } catch (Exception e) {
                SomeGoogly.LOGGER.error("Failed to parse eye config {}", entry.getKey(), e);
            }
        }
        ServerEyeConfigs.replaceAll(parsed);
        SomeGoogly.LOGGER.info("Loaded {} eye configs", parsed.size());
    }
}
