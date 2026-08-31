package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.picker.forge.ForgePickerClient;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.forge.ForgeClientConfig;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.network.forge.ForgeClientNetworkTransport;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Physical-client bootstrap kept out of the dedicated-server entry point. */
public final class ForgeClientBootstrap {

    private static ClientEventHandler clientEventHandler;

    private ForgeClientBootstrap() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ClientNetworkHandler.register();
        ForgeClientNetworkTransport.register();
        ClientCommandRegistrationEvent.EVENT.register(GooglyClientCommands::register);
        ForgeClientConfig.register(context);

        MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new EyeInspectIndicator());
        modBus.addListener(SlimyEyeColors::register);
        modBus.addListener(ForgeClientBootstrap::addLayers);
        modBus.addListener(ForgePickerClient::registerHud);
        modBus.addListener(ForgePickerClient::registerKeys);
        MinecraftForge.EVENT_BUS.register(new ForgePickerClient());
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        clientEventHandler.addLayers();
    }
}
