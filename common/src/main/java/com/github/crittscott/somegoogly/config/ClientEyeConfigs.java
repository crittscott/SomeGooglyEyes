package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Client-side store of the resolved server eye state, populated from the server via
 * {@code EyeConfigSyncPacket}: the per-entity eye geometry configs, the current
 * {@code googlyEyesEnabled} master switch, and the behavior settings clients run behaviors from. This store also owns the resolved-view cache used by
 * renderers.
 *
 * <p>Separate from {@link ServerEyeConfigs} to avoid single-player static-state bleed: in SP the
 * integrated server fills the server store and the local sync fills this one independently. Installed
 * maps are immutable snapshots; their mutable model values are owned by this store and must be treated
 * as read-only after installation.
 */
public final class ClientEyeConfigs {

    private static volatile BehaviorSettings behaviors = BehaviorSettings.NONE;
    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();
    // Defaults to true so a client isn't wrongly suppressed before its first sync from the server lands.
    private static volatile boolean googlyEyesEnabled = true;
    // Nested by entity then a packed (variant, baby) key, mirroring ModelMemo's per-frame-allocation-free
    // pattern: a compound key object would have to be allocated on every call just to probe the cache.
    private static final Map<ResourceLocation, Map<Integer, HeadInfo>> resolved = new HashMap<>();

    private ClientEyeConfigs() {
    }

    /**
     * The immutable synced map, by entity. Callers may retain the snapshot across replacements but must
     * not mutate its {@link RuntimeConfigSet} values.
     */
    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return configs;
    }

    /** The server's behavior settings, as of the last sync. */
    public static BehaviorSettings behaviors() {
        return behaviors;
    }

    /** The cached view of {@code config}'s {@code variant} for {@code entity}, created on first use. */
    private static HeadInfo cached(ResourceLocation entity, boolean baby, @Nullable RuntimeConfig config,
                                   int variant) {
        int key = (variant << 1) | (baby ? 1 : 0);
        Map<Integer, HeadInfo> byVariant = resolved.computeIfAbsent(entity, e -> new HashMap<>());
        HeadInfo cached = byVariant.get(key);
        if (cached != null) {
            return cached;
        }
        HeadInfo created = new HeadInfo(config, variant);
        byVariant.put(key, created);
        return created;
    }

    /** Clear everything (e.g. on disconnect) so a previous server's configs don't leak. */
    public static void clear() {
        behaviors = BehaviorSettings.NONE;
        configs = Collections.emptyMap();
        googlyEyesEnabled = true;
        resolved.clear();
    }

    /** The server's current {@code googlyEyesEnabled} master switch, as of the last sync. */
    public static boolean googlyEyesEnabled() {
        return googlyEyesEnabled;
    }

    /** Return the entity's config for the requested age, including the age-independent fallback. */
    @Nullable
    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }

    /** Resolve and cache the selected variant for the requested age, for client rendering. */
    public static HeadInfo resolve(ResourceLocation entity, boolean baby, float variantRoll) {
        RuntimeConfig config = get(entity, baby);
        return cached(entity, baby, config, EyeConfigModel.chooseVariantIndex(config, variantRoll));
    }

    /** Resolve and cache the selected variant for {@code living}'s current age. */
    public static HeadInfo resolve(ResourceLocation entity, LivingEntity living, float variantRoll) {
        return resolve(entity, living.isBaby(), variantRoll);
    }

    /**
     * Resolve and cache the variant a placed head of {@code entity}'s kind shows
     * ({@link SkullEyeState#variantIndex}), for client rendering.
     */
    public static HeadInfo resolveHead(ResourceLocation entity, float variantRoll) {
        RuntimeConfig config = get(entity, false);
        return cached(entity, false, config, SkullEyeState.variantIndex(config, variantRoll));
    }

    /**
     * Install an immutable top-level snapshot from server synchronization and invalidate every resolved
     * rendering view. The supplied model values become store-owned and must not be mutated afterward.
     */
    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next, boolean googlyEyesEnabled,
                                  BehaviorSettings behaviors) {
        configs = Map.copyOf(next);
        ClientEyeConfigs.behaviors = behaviors;
        ClientEyeConfigs.googlyEyesEnabled = googlyEyesEnabled;
        resolved.clear();
        SomeGooglyCommon.LOGGER.info("Received {} eye configs from the server (googlyEyesEnabled={})",
                configs.size(), googlyEyesEnabled);
    }
}
