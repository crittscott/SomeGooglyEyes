package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Server-authoritative store of eye geometry configs, loaded from datapacks by
 * {@link EyeConfigReloadListener}. The server uses this to gate {@code hasGooglyEyes}
 * (see {@link ServerServices#onLivingEntityLoaded}) and to build the sync payload sent to clients.
 *
 * <p>Kept separate from {@link ClientEyeConfigs} so the integrated server and client don't share
 * one static map in single-player. Installed maps are immutable snapshots; their mutable model
 * values are owned by this store and must be treated as read-only after installation.
 */
public final class ServerEyeConfigs {

    /**
     * Hard-excluded from googly eyes: the ender dragon's renderer bypasses the
     * {@code LivingEntityRenderer} family entirely (no eye layer can attach) and its model has no
     * walkable part tree, so eyes can never render on it. {@link EyeConfigReloadListener} refuses
     * datapack configs for it and {@code /sg spawnall} skips it.
     */
    public static final ResourceLocation ENDER_DRAGON =
            ResourceLocation.fromNamespaceAndPath("minecraft", "ender_dragon");

    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();
    private static volatile long generation;
    private static volatile String signature = "";

    private ServerEyeConfigs() {
    }

    /**
     * The immutable installed map. Callers may retain the snapshot across replacements but must not
     * mutate its {@link RuntimeConfigSet} values.
     */
    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return configs;
    }

    /**
     * Whether this entity can wear eyes at <b>any</b> life stage (baby or adult). Used by the at-spawn
     * roll ({@link ServerServices#onLivingEntityLoaded}): that decision is stored for life, so a baby
     * that only has an adult config must still be allowed to roll — otherwise it stores
     * {@code hasGooglyEyes=false} and never re-rolls, locking it out of eyes forever even after it
     * grows up. The client swaps in the age-appropriate geometry as the mob ages.
     */
    public static boolean canEverWearEyes(LivingEntity living) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        return RuntimeConfig.isUsable(get(type, false)) || RuntimeConfig.isUsable(get(type, true));
    }

    /** Return the entity's config for the requested age, including the age-independent fallback. */
    @Nullable
    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }

    /** Return the entity's config for {@code living}'s current age. */
    @Nullable
    public static RuntimeConfig get(ResourceLocation entity, LivingEntity living) {
        return get(entity, living.isBaby());
    }

    /** Resolve the selected variant without sharing the client's renderer cache. */
    public static HeadInfo resolve(ResourceLocation entity, LivingEntity living, float variantRoll) {
        RuntimeConfig config = get(entity, living);
        return new HeadInfo(config, EyeConfigModel.chooseVariantIndex(config, variantRoll));
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

    /**
     * Force-install a snapshot, discard its content signature, and advance the generation. This is the
     * unconditional replacement path used by tests; normal datapack reload uses
     * {@link #replaceIfChanged}.
     */
    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        configs = Map.copyOf(next);
        signature = "";
        generation++;
    }

    /**
     * Datapack-reload entry point: swap in the resolved set and bump {@link #generation} only when
     * {@code nextSignature} (a canonical serialization of {@code next}, computed by the reload
     * listener) differs from the installed set's. A {@code /reload} triggered for an unrelated
     * datapack thus stops re-fanning the whole eye-config snapshot to every online player. Returns
     * whether a swap happened.
     */
    public static boolean replaceIfChanged(Map<ResourceLocation, RuntimeConfigSet> next, String nextSignature) {
        if (nextSignature.equals(signature)) {
            return false;
        }
        configs = Map.copyOf(next);
        signature = nextSignature;
        generation++;
        return true;
    }

    /** Drop the content signature so the first reload of the next world always resynchronizes. */
    public static void onServerStopping() {
        signature = "";
    }
}
