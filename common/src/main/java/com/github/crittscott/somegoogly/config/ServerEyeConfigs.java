package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import org.jspecify.annotations.Nullable;
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
    public static final Identifier ENDER_DRAGON =
            EntityType.getKey(EntityType.ENDER_DRAGON);

    private record Installed(Map<Identifier, RuntimeConfigSet> configs,
                             Map<Identifier, CompoundTag> encoded) {
    }

    private static final Installed EMPTY = new Installed(Map.of(), Map.of());

    private static volatile Installed installed = EMPTY;

    private ServerEyeConfigs() {
    }

    /**
     * The immutable installed map. Callers may retain the snapshot across replacements but must not
     * mutate its {@link RuntimeConfigSet} values.
     */
    public static Map<Identifier, RuntimeConfigSet> all() {
        return installed.configs();
    }

    /** The installed sets encoded through {@link RuntimeConfigSet#CODEC}; read-only, like {@link #all}. */
    public static Map<Identifier, CompoundTag> encoded() {
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
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        return RuntimeConfig.isUsable(get(type, false)) || RuntimeConfig.isUsable(get(type, true));
    }

    /** Return the entity's config for the requested age, including the age-independent fallback. */
    @Nullable
    public static RuntimeConfig get(Identifier entity, boolean baby) {
        RuntimeConfigSet set = installed.configs().get(entity);
        return set == null ? null : set.get(baby);
    }

    /** Resolve the selected variant for the requested age without sharing the client's renderer cache. */
    public static HeadInfo resolve(Identifier entity, boolean baby, float variantRoll) {
        RuntimeConfig config = get(entity, baby);
        return new HeadInfo(config, EyeConfigModel.chooseVariantIndex(config, variantRoll));
    }

    /** Resolve the variant a placed head of {@code entity}'s kind shows ({@link SkullEyeState#variantIndex}). */
    public static HeadInfo resolveHead(Identifier entity, float variantRoll) {
        RuntimeConfig config = get(entity, false);
        return new HeadInfo(config, SkullEyeState.variantIndex(config, variantRoll));
    }

    /** Encode each set through {@link RuntimeConfigSet#CODEC} as NBT. */
    public static Map<Identifier, CompoundTag> encode(Map<Identifier, RuntimeConfigSet> configs) {
        Map<Identifier, CompoundTag> encoded = new HashMap<>();
        for (Map.Entry<Identifier, RuntimeConfigSet> entry : configs.entrySet()) {
            encoded.put(entry.getKey(), (CompoundTag) RuntimeConfigSet.CODEC
                    .encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow());
        }
        return Map.copyOf(encoded);
    }

    /**
     * Datapack-reload entry point: install {@code next} only when its encoding differs from the installed
     * set's, so a {@code /reload} for an unrelated datapack leaves the installed snapshot, and therefore
     * every client's copy, alone. Returns whether a swap happened.
     */
    public static boolean replaceIfChanged(Map<Identifier, RuntimeConfigSet> next,
                                           Map<Identifier, CompoundTag> nextEncoded) {
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
