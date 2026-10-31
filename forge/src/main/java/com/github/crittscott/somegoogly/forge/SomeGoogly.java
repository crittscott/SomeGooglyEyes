package com.github.crittscott.somegoogly.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.forge.ForgeClientBootstrap;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.forge.ForgeServerConfig;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import com.github.crittscott.somegoogly.registry.forge.ForgeContentRegistrar;
import com.github.crittscott.somegoogly.server.forge.ForgeServerEvents;
import com.github.crittscott.somegoogly.server.forge.HeadEyesLootModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge bootstrap for common registration and server datapack resources. */
@Mod(SomeGooglyCommon.MOD_ID)
public final class SomeGoogly {

    public SomeGoogly(FMLJavaModLoadingContext context) {
        // Eye configs are loaded from datapacks on the server (EyeConfigReloadListener) and synced
        // to clients (EyeConfigSyncPacket); nothing to load at construction time.
        BusGroup modBusGroup = context.getModBusGroup();
        ForgeContentRegistrar registrar = new ForgeContentRegistrar();
        SomeGooglyCommon.init(registrar);
        registrar.register(modBusGroup);
        HeadEyesLootModifier.register(modBusGroup);
        ForgeNetworkTransport.register();
        ForgeServerConfig.register(context);

        ForgeServerEvents.register();
        AddReloadListenerEvent.BUS.addListener(SomeGoogly::addReloadListeners);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientBootstrap.register(context);
        }

        SomeGooglyCommon.LOGGER.info("{} initialized on Forge", SomeGooglyCommon.MOD_NAME);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EyeConfigReloadListener());
    }
}
