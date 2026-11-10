package com.github.crittscott.somegoogly.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.forge.ForgeClientBootstrap;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.forge.ForgeServerConfig;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.platform.forge.RegistrationImpl;
import com.github.crittscott.somegoogly.server.forge.ForgeServerEvents;
import com.github.crittscott.somegoogly.server.forge.HeadEyesLootModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge bootstrap for common registration and server datapack resources. */
@Mod(SomeGooglyCommon.MOD_ID)
public final class SomeGooglyForge {

    public SomeGooglyForge(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModContent.init();
        RegistrationImpl.attach(modEventBus);
        HeadEyesLootModifier.register(modEventBus);
        ForgeNetworkTransport.register();
        ForgeServerConfig.register(context);

        ForgeServerEvents.register(MinecraftForge.EVENT_BUS);
        MinecraftForge.EVENT_BUS.addListener(SomeGooglyForge::addReloadListeners);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientBootstrap.register(context);
        }

        SomeGooglyCommon.LOGGER.info("{} {} registered on Forge", SomeGooglyCommon.MOD_NAME,
                ModVersionLookup.modVersion(SomeGooglyCommon.MOD_ID).orElseThrow());
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EyeConfigReloadListener());
    }
}
