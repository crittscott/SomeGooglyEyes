package com.github.crittscott.somegoogly.client.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeClientConfig;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Physical-client registration for NeoForge client services. */
public final class NeoForgeClient {

    private NeoForgeClient() {
    }

    /** Register the client config and every client-side NeoForge listener. */
    public static void register(IEventBus modBus, IEventBus gameBus, ModContainer modContainer) {
        NeoForgeClientConfig.register(modBus, modContainer);

        modBus.addListener(NeoForgeClient::addRendererLayers);
        modBus.addListener(NeoForgeClient::registerItemColors);
        modBus.addListener(NeoForgeClient::registerClientExtensions);
        modBus.addListener(NeoForgeClient::registerGuiLayers);
        modBus.addListener(NeoForgeClient::registerKeyMappings);

        gameBus.addListener(NeoForgeClient::registerClientCommands);
        gameBus.addListener(NeoForgeClient::onClientTick);
        gameBus.addListener(NeoForgeClient::onLoggingOut);
        gameBus.addListener(NeoForgeClient::onEntityLeaveLevel);
        gameBus.addListener(NeoForgeClient::onLevelUnload);
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

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> EyeItemProperties.slimyEyeTint(stack, tintIndex),
                ModContent.SLIMY_EYE.get());
    }

    /** Render the Googly Eye as the real 3D eye model (tinted by its AppearanceOverride, googly when held). */
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private GooglyEyeItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GooglyEyeItemRenderer();
                }
                return renderer;
            }
        }, ModContent.GOOGLY_EYE.get());
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

    private static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ClientEyeRuntime.onEntityRemoved(living);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientEyeRuntime.onLevelChanged();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLifecycle.onDisconnect();
    }
}
