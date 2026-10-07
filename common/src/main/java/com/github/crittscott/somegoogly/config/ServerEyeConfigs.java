package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-authoritative store of eye geometry configs, loaded from datapacks by
 * {@link EyeConfigReloadListener}. The server uses this to gate {@code hasGooglyEyes}
 * (see {@link ServerServices#onLivingEntityLoaded}) and to build the sync payload sent to clients.
 *
 * <p>Kept separate from {@link ClientEyeConfigs} so the integrated server and client don't share
 * one static map in single-player. Installed maps are immutable snapshots; their mutable model
 * values are owned by this store and must be treated as read-only after installation. Each snapshot
 * carries its sets' NBT encoding, which is both the sync payload content and the identity a reload
 * compares to decide whether anything changed.
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

    private record Installed(Map<ResourceLocation, RuntimeConfigSet> configs,
                             Map<ResourceLocation, CompoundTag> encoded) {
    }

    private static final Installed EMPTY = new Installed(Map.of(), Map.of());

    private static volatile Installed installed = EMPTY;

    private ServerEyeConfigs() {
    }

    /**
     * The immutable installed map. Callers may retain the snapshot across replacements but must not
     * mutate its {@link RuntimeConfigSet} values.
     */
    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return installed.configs();
    }

    /** The installed sets encoded through {@link RuntimeConfigSet#CODEC}; read-only, like {@link #all}. */
    public static Map<ResourceLocation, CompoundTag> encoded() {
        return installed.encoded();
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
        RuntimeConfigSet set = installed.configs().get(entity);
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

    /**
     * Whether this entity can wear eyes <b>right now, at its current age</b>: it has an age-appropriate
     * config that is enabled and has at least one head. Used by the slimy eye ({@code SlimyEyeItem}),
     * which should only apply to targets the eyes would visibly appear on immediately. Players have a
     * definition ({@code player.json}) and so are eligible; only an unconfigured entity is not.
     */
    public static boolean isEligible(LivingEntity living) {
        return RuntimeConfig.isUsable(get(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()), living));
    }

    /** Encode each set through {@link RuntimeConfigSet#CODEC} as NBT. */
    public static Map<ResourceLocation, CompoundTag> encode(Map<ResourceLocation, RuntimeConfigSet> configs) {
        Map<ResourceLocation, CompoundTag> encoded = new HashMap<>();
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : configs.entrySet()) {
            encoded.put(entry.getKey(), (CompoundTag) RuntimeConfigSet.CODEC
                    .encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow());
        }
        return Map.copyOf(encoded);
    }

    /** Unconditionally install a snapshot. This is the replacement path used by tests. */
    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        installed = new Installed(Map.copyOf(next), encode(next));
    }

    /**
     * Datapack-reload entry point: install {@code next} only when its encoding differs from the installed
     * set's, so a {@code /reload} for an unrelated datapack leaves the installed snapshot, and therefore
     * every client's copy, alone. Returns whether a swap happened.
     */
    public static boolean replaceIfChanged(Map<ResourceLocation, RuntimeConfigSet> next,
                                           Map<ResourceLocation, CompoundTag> nextEncoded) {
        if (nextEncoded.equals(installed.encoded())) {
            return false;
        }
        installed = new Installed(Map.copyOf(next), Map.copyOf(nextEncoded));
        return true;
    }

    /** Drop the installed set so nothing carries over to the next world. */
    public static void onServerStopping() {
        installed = EMPTY;
    }
}
