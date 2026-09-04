package com.github.crittscott.somegoogly.client.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeClientConfig;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.network.neoforge.NeoForgeClientNetworkTransport;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Physical-client registration for NeoForge client services. */
public final class NeoForgeClient {

    private NeoForgeClient() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus, ModContainer modContainer) {
        // Architectury receivers must exist before NeoForge freezes payload registrations.
        ClientNetworkHandler.register();
        NeoForgeClientNetworkTransport.register();
        NeoForgeClientConfig.register(modBus, modContainer);

        modBus.addListener(NeoForgeClient::addRendererLayers);
        modBus.addListener(NeoForgeClient::registerItemColors);
        modBus.addListener(NeoForgeClient::registerGuiLayers);
        modBus.addListener(NeoForgeClient::registerKeyMappings);

        gameBus.addListener(NeoForgeClient::registerClientCommands);
        gameBus.addListener(NeoForgeClient::onClientTick);
        gameBus.addListener(NeoForgeClient::onEntityJoin);
        gameBus.addListener(NeoForgeClient::onLoggingOut);
    }

    private static void addRendererLayers(EntityRenderersEvent.AddLayers event) {
        ClientRenderLayers.install(Minecraft.getInstance().getEntityRenderDispatcher());
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        GooglyClientCommands.register(event.getDispatcher());
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> EyeItemProperties.slimyEyeTint(stack, tintIndex),
                ModItems.SLIMY_EYE.get());
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "picker"),
                (graphics, partialTick) -> PickerHud.render(
                        graphics, graphics.guiWidth(), graphics.guiHeight()));
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PickerKeys.LOCK);
        event.register(PickerKeys.PART_NEXT);
        event.register(PickerKeys.PART_PREV);
        event.register(PickerKeys.TOGGLE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientLifecycle.tick();
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ClientNetworkHandler.onEntityLoaded(event.getEntity());
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLifecycle.onDisconnect();
    }
}
