package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.server.ServerServices;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-authoritative store of eye geometry configs, loaded from datapacks by
 * {@link EyeConfigReloadListener}. The server uses this to gate {@code hasGooglyEyes}
 * (see {@link ServerServices#onLivingEntityLoaded}) and to build the sync payload sent to clients.
 *
 * <p>Kept separate from {@link ClientEyeConfigs} so the integrated server and client don't share
 * one static map in single-player. The installed set is held as the immutable sync packet built for it; its
 * mutable model values are owned by this store and must be treated as read-only after installation. The packet
 * carries the sets' NBT encoding, which is the identity a reload compares to decide whether anything changed,
 * and its already-compressed bytes, which every joining player and every broadcast reuses.
 */
public final class ServerEyeConfigs {

    private static final EyeConfigSyncPacket EMPTY = new EyeConfigSyncPacket(Map.of(), Map.of());

    /**
     * Hard-excluded from googly eyes: the ender dragon's renderer bypasses the
     * {@code LivingEntityRenderer} family entirely (no eye layer can attach) and its model has no
     * walkable part tree, so eyes can never render on it. {@link EyeConfigReloadListener} refuses
     * datapack configs for it and {@code /sg spawnall} skips it.
     */
    public static final ResourceLocation ENDER_DRAGON =
            EntityType.getKey(EntityType.ENDER_DRAGON);

    private static volatile EyeConfigSyncPacket installed = EMPTY;

    private ServerEyeConfigs() {
    }

    /**
     * The immutable installed map. Callers may retain the snapshot across replacements but must not
     * mutate its {@link RuntimeConfigSet} values.
     */
    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return installed.configs();
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

    /** Encode each set through {@link RuntimeConfigSet#CODEC} as NBT. */
    public static Map<ResourceLocation, CompoundTag> encode(Map<ResourceLocation, RuntimeConfigSet> configs) {
        Map<ResourceLocation, CompoundTag> encoded = new HashMap<>();
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : configs.entrySet()) {
            encoded.put(entry.getKey(), (CompoundTag) RuntimeConfigSet.CODEC
                    .encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow(false, error -> { }));
        }
        return Map.copyOf(encoded);
    }

    /** Return the entity's config for the requested age, including the age-independent fallback. */
    @Nullable
    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = installed.configs().get(entity);
        return set == null ? null : set.get(baby);
    }

    /** Entity kinds for which the renderer cannot display eyes. */
    public static boolean isExcluded(ResourceLocation type) {
        return ENDER_DRAGON.equals(type);
    }

    /** Drop the installed set so nothing carries over to the next world. */
    public static void onServerStopping() {
        installed = EMPTY;
    }

    /** The installed set as the packet every client receives. */
    public static EyeConfigSyncPacket packet() {
        return installed;
    }

    /** The sync packet for {@code configs}, serialized and compressed once, to check and then install. */
    public static EyeConfigSyncPacket packetFor(Map<ResourceLocation, RuntimeConfigSet> configs) {
        return new EyeConfigSyncPacket(configs, encode(configs));
    }

    /**
     * Datapack-reload entry point: install {@code next} only when its encoding differs from the installed
     * set's, so a {@code /reload} for an unrelated datapack leaves the installed snapshot, and therefore
     * every client's copy, alone. Returns whether a swap happened.
     */
    public static boolean replaceIfChanged(EyeConfigSyncPacket next) {
        if (next.encoded().equals(installed.encoded())) {
            return false;
        }
        installed = next;
        return true;
    }

    /** Resolve the selected variant for the requested age without sharing the client's renderer cache. */
    public static HeadInfo resolve(ResourceLocation entity, boolean baby, float variantRoll) {
        RuntimeConfig config = get(entity, baby);
        return new HeadInfo(config, EyeConfigModel.chooseVariantIndex(config, variantRoll));
    }

    /** Resolve the variant a placed head of {@code entity}'s kind shows ({@link SkullEyeState#variantIndex}). */
    public static HeadInfo resolveHead(ResourceLocation entity, float variantRoll) {
        RuntimeConfig config = get(entity, false);
        return new HeadInfo(config, SkullEyeState.variantIndex(entity, config, variantRoll));
    }
}
