package com.github.crittscott.somegoogly.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.fabric.FabricServerConfig;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport;
import com.github.crittscott.somegoogly.platform.fabric.EntityPersistentDataImpl;
import com.github.crittscott.somegoogly.server.fabric.FabricServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

/**
 * Fabric entry point for shared content plus Fabric configuration, resources, and server events.
 */
public final class SomeGooglyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModContent.init();
        EntityPersistentDataImpl.register();
        FabricNetworkTransport.register();
        FabricServerConfig.register();
        FabricServerEvents.register();
        ResourceLoader.get(PackType.SERVER_DATA)
                .registerReloadListener(EyeConfigReloadListener.ID, new EyeConfigReloadListener());

        SomeGooglyCommon.LOGGER.info("{} initialized on Fabric", SomeGooglyCommon.MOD_NAME);
    }
}
