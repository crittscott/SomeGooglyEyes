package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.forge.ForgeClientConfig;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Physical-client registration kept out of the dedicated-server entry point. */
public final class ForgeClientBootstrap {

    private ForgeClientBootstrap() {
    }

    /**
     * Forge's event exposes renderers per type but not the set of types that have one, so walk the
     * entity-type registry. The getter's living bound is generic only; non-living renderers come back too.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addRendererLayers(EntityRenderersEvent.AddLayers event) {
        ClientRenderLayers.clearCaches();
        for (PlayerSkin.Model skin : event.getSkins()) {
            ClientRenderLayers.install(event.getPlayerSkin(skin));
        }
        for (EntityType entityType : BuiltInRegistries.ENTITY_TYPE) {
            EntityRenderer<?> renderer = event.getEntityRenderer(entityType);
            if (renderer != null) {
                ClientRenderLayers.install(renderer);
            }
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLifecycle.tick();
    }

    private static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ClientEyeRuntime.onEntityRemoved(event.getEntity());
        }
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        EyeItemProperties.appendHeadTooltip(event.getItemStack(), event.getToolTip());
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientEyeRuntime.onLevelChanged();
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLifecycle.onDisconnect();
    }

    /** Register the client config and every client-side Forge listener. */
    public static void register(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        IEventBus gameBus = MinecraftForge.EVENT_BUS;

        ForgeClientConfig.register(context);

        modBus.addListener(ForgeClientBootstrap::addRendererLayers);
        modBus.addListener(ForgeClientBootstrap::registerItemColors);
        modBus.addListener(ForgeClientBootstrap::registerGuiLayers);
        modBus.addListener(ForgeClientBootstrap::registerKeyMappings);

        gameBus.addListener(ForgeClientBootstrap::registerClientCommands);
        gameBus.addListener(ForgeClientBootstrap::onClientTick);
        gameBus.addListener(ForgeClientBootstrap::onLoggingOut);
        gameBus.addListener(ForgeClientBootstrap::onEntityLeaveLevel);
        gameBus.addListener(ForgeClientBootstrap::onItemTooltip);
        gameBus.addListener(ForgeClientBootstrap::onLevelUnload);
        if (GeckoCompat.isLoaded()) {
            ForgeGeckoLayers.register(gameBus);
        }
        ClientLifecycle.logStartup();
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        GooglyClientCommands.register(event.getDispatcher());
    }

    private static void registerGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "picker"),
                (graphics, partialTick) -> PickerHud.render(graphics, graphics.guiWidth()));
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> EyeItemProperties.slimyEyeTint(stack, tintIndex),
                ModContent.SLIMY_EYE.get());
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PickerInput.LOCK);
        event.register(PickerInput.PART_NEXT);
        event.register(PickerInput.PART_PREV);
        event.register(PickerInput.TOGGLE);
    }
}
