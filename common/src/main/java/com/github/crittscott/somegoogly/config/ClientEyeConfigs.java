package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side store of eye geometry configs, populated from the server via
 * {@code EyeConfigSyncPacket}. This store also owns the resolved-view cache used by renderers.
 *
 * <p>Separate from {@link ServerEyeConfigs} to avoid single-player static-state bleed: in SP the
 * integrated server fills the server store and the local sync fills this one independently. Installed
 * maps are immutable snapshots; their mutable model values are owned by this store and must be treated
 * as read-only after installation.
 */
public final class ClientEyeConfigs {

    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();
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

    /** Clear everything (e.g. on disconnect) so a previous server's configs don't leak. */
    public static void clear() {
        configs = Collections.emptyMap();
        resolved.clear();
    }

    /** Return the entity's config for the requested age, including the age-independent fallback. */
    @Nullable
    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }

    /** Resolve and cache the selected variant for client rendering. */
    public static HeadInfo resolve(ResourceLocation entity, LivingEntity living, float variantRoll) {
        boolean baby = living.isBaby();
        RuntimeConfig config = get(entity, baby);
        int variant = EyeConfigModel.chooseVariantIndex(config, variantRoll);
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

    /**
     * Install an immutable top-level snapshot from server synchronization and invalidate every resolved
     * rendering view. The supplied model values become store-owned and must not be mutated afterward.
     */
    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        configs = Map.copyOf(next);
        resolved.clear();
    }
}
