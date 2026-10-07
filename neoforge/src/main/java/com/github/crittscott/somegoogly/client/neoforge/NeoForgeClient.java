package com.github.crittscott.somegoogly.client.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.SlimyEyeIrisTint;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeClientConfig;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

/** Physical-client registration for NeoForge client services. */
public final class NeoForgeClient {

    private NeoForgeClient() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus, ModContainer modContainer) {
        NeoForgeClientConfig.register(modBus, modContainer);

        modBus.addListener(NeoForgeClient::addRendererLayers);
        modBus.addListener(NeoForgeClient::registerItemTintSources);
        modBus.addListener(NeoForgeClient::registerSpecialModelRenderers);
        modBus.addListener(NeoForgeClient::registerGuiLayers);
        modBus.addListener(NeoForgeClient::registerKeyMappings);

        gameBus.addListener(NeoForgeClient::registerClientCommands);
        gameBus.addListener(NeoForgeClient::onClientTick);
        gameBus.addListener(NeoForgeClient::onLoggingOut);
        if (GeckoCompat.isLoaded()) {
            NeoForgeGeckoLayers.register(gameBus);
        }
    }

    private static void addRendererLayers(EntityRenderersEvent.AddLayers event) {
        ClientRenderLayers.clearCaches();
        for (PlayerSkin.Model skin : event.getSkins()) {
            ClientRenderLayers.install(event.getSkin(skin));
        }
        for (EntityType<?> entityType : event.getEntityTypes()) {
            ClientRenderLayers.install(event.getRenderer(entityType));
        }
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        GooglyClientCommands.register(event.getDispatcher());
    }

    private static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(SlimyEyeIrisTint.ID, SlimyEyeIrisTint.MAP_CODEC);
    }

    private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(GooglyEyeItemRenderer.ID, GooglyEyeItemRenderer.Unbaked.MAP_CODEC);
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

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLifecycle.onDisconnect();
    }
}
