package com.github.crittscott.somegoogly;

import com.github.crittscott.somegoogly.config.FabricServerConfig;
import com.github.crittscott.somegoogly.config.FabricEyeConfigReloadListener;
import com.github.crittscott.somegoogly.server.FabricServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

/**
 * Fabric entry point for shared content plus Fabric configuration, resources, and server events.
 */
public final class SomeGooglyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SomeGooglyCommon.init();
        FabricServerConfig.register();
        FabricServerEvents.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new FabricEyeConfigReloadListener());

        SomeGooglyCommon.LOGGER.info("{} initializing on Fabric", SomeGooglyCommon.MOD_NAME);
    }
}
