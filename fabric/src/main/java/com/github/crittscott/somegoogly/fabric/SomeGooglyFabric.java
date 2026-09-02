package com.github.crittscott.somegoogly.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.fabric.FabricServerConfig;
import com.github.crittscott.somegoogly.config.fabric.FabricEyeConfigReloadListener;
import com.github.crittscott.somegoogly.registry.fabric.FabricContentRegistrar;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport;
import com.github.crittscott.somegoogly.server.fabric.FabricServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

/**
 * Fabric entry point for shared content plus Fabric configuration, resources, and server events.
 */
public final class SomeGooglyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SomeGooglyCommon.init(new FabricContentRegistrar());
        FabricNetworkTransport.register();
        FabricServerConfig.register();
        FabricServerEvents.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new FabricEyeConfigReloadListener());

        SomeGooglyCommon.LOGGER.info("{} initialized on Fabric", SomeGooglyCommon.MOD_NAME);
    }
}
