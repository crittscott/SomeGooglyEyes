package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side store of eye geometry configs, populated from the server via
 * {@code EyeConfigSyncPacket}. This store also owns the resolved-view cache used by renderers.
 *
 * <p>Separate from {@link ServerEyeConfigs} to avoid single-player static-state bleed: in SP the
 * integrated server fills the server store and the local sync fills this one independently.
 */
public final class ClientEyeConfigs {

    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();
    private static final Map<CacheKey, HeadInfo> resolved = new HashMap<>();

    private record CacheKey(ResourceLocation entity, boolean baby, int variant) {
    }

    private ClientEyeConfigs() {
    }

    /** The whole synced config set, by entity (used by the picker's {@code exportall} dump). */
    public static Map<ResourceLocation, RuntimeConfigSet> all() {
        return configs;
    }

    /** Clear everything (e.g. on disconnect) so a previous server's configs don't leak. */
    public static void clear() {
        configs = Collections.emptyMap();
        resolved.clear();
    }

    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }

    /** Resolve and cache the selected variant for client rendering. */
    public static HeadInfo resolve(ResourceLocation entity, LivingEntity living, float variantRoll) {
        boolean baby = living.isBaby();
        RuntimeConfig config = get(entity, baby);
        int variant = EyeConfigModel.chooseVariantIndex(config, variantRoll);
        return resolved.computeIfAbsent(new CacheKey(entity, baby, variant),
                key -> new HeadInfo(config, key.variant()));
    }

    /** Replace the client's configs (e.g. on datapack sync) and invalidate dependent caches. */
    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        configs = next;
        resolved.clear();
    }
}
