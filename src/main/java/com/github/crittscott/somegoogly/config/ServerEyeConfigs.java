package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.head.HeadInfo.EntityConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * Server-authoritative store of eye geometry configs, loaded from datapacks by
 * {@link EyeConfigReloadListener}. The server uses this to gate {@code hasGooglyEyes}
 * (see {@code ServerEventHandler}) and to build the sync payload sent to clients.
 *
 * <p>Kept separate from {@link ClientEyeConfigs} so the integrated server and client don't share
 * one static map in single-player.
 */
public final class ServerEyeConfigs {

    private static volatile Map<ResourceLocation, EntityConfig> configs = Collections.emptyMap();

    private ServerEyeConfigs() {
    }

    public static void replaceAll(Map<ResourceLocation, EntityConfig> next) {
        configs = next;
    }

    public static EntityConfig get(ResourceLocation entity) {
        return configs.get(entity);
    }

    public static Map<ResourceLocation, EntityConfig> all() {
        return configs;
    }
}
