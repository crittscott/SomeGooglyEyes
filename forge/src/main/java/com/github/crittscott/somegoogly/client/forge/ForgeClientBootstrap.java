package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.forge.ForgeClientConfig;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Physical-client registration kept out of the dedicated-server entry point. */
public final class ForgeClientBootstrap {

    private ForgeClientBootstrap() {
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
        gameBus.addListener(ForgeClientBootstrap::onLevelUnload);
        if (GeckoCompat.isLoaded()) {
            ForgeGeckoLayers.register(gameBus);
        }
    }

    /**
     * Forge's event exposes renderers per type but not the set of types that have one, so walk the
     * entity-type registry. Its getter casts to a living renderer, so a non-living type's renderer fails
     * that cast; those types carry no vanilla-model eye layer and are skipped.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addRendererLayers(EntityRenderersEvent.AddLayers event) {
        ClientRenderLayers.clearCaches();
        for (String skin : event.getSkins()) {
            ClientRenderLayers.install(event.getSkin(skin));
        }
        for (EntityType entityType : BuiltInRegistries.ENTITY_TYPE) {
            EntityRenderer<?> renderer;
            try {
                renderer = event.getRenderer(entityType);
            } catch (ClassCastException notLiving) {
                continue;
            }
            if (renderer != null) {
                ClientRenderLayers.install(renderer);
            }
        }
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        GooglyClientCommands.register(event.getDispatcher());
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> EyeItemProperties.slimyEyeTint(stack, tintIndex),
                ModContent.SLIMY_EYE.get());
    }

    private static void registerGuiLayers(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("picker",
                (gui, graphics, partialTick, width, height) -> PickerHud.render(graphics, width, height));
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PickerKeys.LOCK);
        event.register(PickerKeys.PART_NEXT);
        event.register(PickerKeys.PART_PREV);
        event.register(PickerKeys.TOGGLE);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
