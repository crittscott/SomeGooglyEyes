package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * Client-side store of eye geometry configs, populated from the server via
 * {@code EyeConfigSyncPacket}. {@link HeadInfo} reads from here.
 *
 * <p>Separate from {@link ServerEyeConfigs} to avoid single-player static-state bleed: in SP the
 * integrated server fills the server store and the local sync fills this one independently.
 */
public final class ClientEyeConfigs {

    private static volatile Map<ResourceLocation, RuntimeConfigSet> configs = Collections.emptyMap();

    private ClientEyeConfigs() {
    }

    /** Replace the client's configs (e.g. on datapack sync) and invalidate dependent caches. */
    public static void replaceAll(Map<ResourceLocation, RuntimeConfigSet> next) {
        configs = next;
        HeadInfo.clearCache();
    }

    /** Clear everything (e.g. on disconnect) so a previous server's configs don't leak. */
    public static void clear() {
        configs = Collections.emptyMap();
        HeadInfo.clearCache();
    }

    public static RuntimeConfig get(ResourceLocation entity, boolean baby) {
        RuntimeConfigSet set = configs.get(entity);
        return set == null ? null : set.get(baby);
    }
}
