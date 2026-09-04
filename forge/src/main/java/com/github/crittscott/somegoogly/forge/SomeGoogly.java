package com.github.crittscott.somegoogly.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.forge.ForgeClientBootstrap;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.forge.ForgeServerConfig;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import com.github.crittscott.somegoogly.registry.forge.ForgeContentRegistrar;
import com.github.crittscott.somegoogly.server.forge.ForgeServerEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge bootstrap for common registration and server datapack resources. */
@Mod(SomeGooglyCommon.MOD_ID)
public final class SomeGoogly {

    public SomeGoogly(FMLJavaModLoadingContext context) {
        // Eye configs are loaded from datapacks on the server (EyeConfigReloadListener) and synced
        // to clients (EyeConfigSyncPacket); nothing to load at construction time.
        IEventBus modEventBus = context.getModEventBus();
        ForgeContentRegistrar registrar = new ForgeContentRegistrar();
        SomeGooglyCommon.init(registrar);
        registrar.register(modEventBus);
        ForgeNetworkTransport.register();
        ForgeServerConfig.register();

        ForgeServerEvents.register(MinecraftForge.EVENT_BUS);
        MinecraftForge.EVENT_BUS.addListener(SomeGoogly::addReloadListeners);

        // Connection compatibility is decided by the mod's own protocol handshake (NetworkHandler),
        // not the display version, so tell Forge not to gate connections on this mod's presence.
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORESERVERONLY,
                (remoteVersion, isServer) -> true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientBootstrap.register(context));

        SomeGooglyCommon.LOGGER.info("{} initialized on Forge", SomeGooglyCommon.MOD_NAME);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EyeConfigReloadListener());
    }
}
