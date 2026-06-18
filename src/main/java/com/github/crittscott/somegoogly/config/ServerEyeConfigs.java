package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfigSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

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

    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();

    private ServerEyeConfigs() {
    }

    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        configs = next;
    }

    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }

    public static RuntimeConfig get(ResourceLocation entity, LivingEntity living) {
        return get(entity, living.isBaby());
    }

    /**
     * Whether this entity can actually wear eyes: it has an age-appropriate config that is enabled and
     * has at least one head. This is the shared eligibility gate used both by the at-spawn roll
     * ({@code ServerEventHandler}) and by the splash potion ({@code EyePotionInteractions}). Players
     * (and any other unconfigured entity) are not eligible.
     */
    public static boolean isEligible(LivingEntity living) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        RuntimeConfig config = get(type, living);
        return config != null && config.isEnabled() && config.heads != null && !config.heads.isEmpty();
    }

    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return configs;
    }
}
