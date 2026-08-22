package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
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

    /**
     * Hard-excluded from googly eyes: the ender dragon's renderer bypasses the
     * {@code LivingEntityRenderer} family entirely (no eye layer can attach) and its model has no
     * walkable part tree, so eyes can never render on it. {@link EyeConfigReloadListener} refuses
     * datapack configs for it and {@code /sg spawnall} skips it.
     */
    public static final ResourceLocation ENDER_DRAGON = new ResourceLocation("minecraft", "ender_dragon");

    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();
    private static volatile long generation;

    private ServerEyeConfigs() {
    }

    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return configs;
    }

    /**
     * Whether this entity can wear eyes at <b>any</b> life stage (baby or adult). Used by the at-spawn
     * roll ({@code ServerEventHandler}): that decision is stored for life, so a baby that only has an
     * adult config must still be allowed to roll — otherwise it stores {@code hasGooglyEyes=false} and
     * never re-rolls, locking it out of eyes forever even after it grows up. The client swaps in the
     * age-appropriate geometry as the mob ages.
     */
    public static boolean canEverWearEyes(LivingEntity living) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        return RuntimeConfig.isUsable(get(type, false)) || RuntimeConfig.isUsable(get(type, true));
    }

    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }

    public static RuntimeConfig get(ResourceLocation entity, LivingEntity living) {
        return get(entity, living.isBaby());
    }

    /** Monotonic identity of the currently installed config map, used by network payload caching. */
    public static long generation() {
        return generation;
    }

    /**
     * Whether this entity can wear eyes <b>right now, at its current age</b>: it has an age-appropriate
     * config that is enabled and has at least one head. Used by the slimy eye ({@code SlimyEyeItem}),
     * which should only apply to targets the eyes would visibly appear on immediately. Players have a
     * definition ({@code player.json}) and so are eligible; only an unconfigured entity is not.
     */
    public static boolean isEligible(LivingEntity living) {
        return RuntimeConfig.isUsable(get(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()), living));
    }

    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        configs = next;
        generation++;
    }
}
