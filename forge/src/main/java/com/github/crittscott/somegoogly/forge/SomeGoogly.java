package com.github.crittscott.somegoogly.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.forge.ForgeClientBootstrap;
import com.github.crittscott.somegoogly.config.forge.ForgeServerConfig;
import com.github.crittscott.somegoogly.event.EyeItemInteractions;
import com.github.crittscott.somegoogly.event.EyeReactionHandler;
import com.github.crittscott.somegoogly.event.ServerEventHandler;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import com.github.crittscott.somegoogly.registry.forge.ForgeContentRegistrar;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entry point for Some Googly Eyes. Registers shared content, configuration, networking, and
 * event handlers, while keeping client-only rendering, input, and picker setup behind a physical-side
 * guard. Datapack eye definitions are loaded later through the server reload lifecycle.
 */
@Mod(SomeGooglyCommon.MOD_ID)
public class SomeGoogly {

    public SomeGoogly(FMLJavaModLoadingContext context) {
        // Eye configs are loaded from datapacks on the server (EyeConfigReloadListener) and synced
        // to clients (EyeConfigSyncPacket); nothing to load at construction time.
        IEventBus modEventBus = context.getModEventBus();
        ForgeContentRegistrar registrar = new ForgeContentRegistrar();
        SomeGooglyCommon.init(registrar);
        registrar.register(modEventBus);
        ForgeNetworkTransport.register();
        ForgeServerConfig.register();

        MinecraftForge.EVENT_BUS.register(new EyeItemInteractions());
        MinecraftForge.EVENT_BUS.register(new EyeReactionHandler());
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());

        context.registerDisplayTest("1", (remoteVersion, isServer) -> true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientBootstrap.register(context));

        SomeGooglyCommon.LOGGER.info("{} initialized on Forge", SomeGooglyCommon.MOD_NAME);
    }
}
