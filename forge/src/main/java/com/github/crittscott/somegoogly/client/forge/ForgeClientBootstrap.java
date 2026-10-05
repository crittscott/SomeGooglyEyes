package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.SlimyEyeIrisTint;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.forge.ForgeClientConfig;
import com.github.crittscott.somegoogly.network.forge.ForgeClientNetworkTransport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Physical-client registration kept out of the dedicated-server entry point. */
public final class ForgeClientBootstrap {

    private ForgeClientBootstrap() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        IEventBus gameBus = MinecraftForge.EVENT_BUS;

        ClientNetworkHandler.register();
        ForgeClientNetworkTransport.register();
        ForgeClientConfig.register(context);
        registerItemModelTypes();

        modBus.addListener(ForgeClientBootstrap::addRendererLayers);
        modBus.addListener(ForgeClientBootstrap::registerGuiLayers);
        modBus.addListener(ForgeClientBootstrap::registerKeyMappings);

        gameBus.addListener(ForgeClientBootstrap::registerClientCommands);
        gameBus.addListener(ForgeClientBootstrap::onClientTick);
        gameBus.addListener(ForgeClientBootstrap::onEntityJoin);
        gameBus.addListener(ForgeClientBootstrap::onLoggingOut);
    }

    private static void addRendererLayers(EntityRenderersEvent.AddLayers event) {
        ClientRenderLayers.install(Minecraft.getInstance().getEntityRenderDispatcher());
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        GooglyClientCommands.register(event.getDispatcher());
    }

    /**
     * Forge has no registration event for item tint sources or special model renderers, so add ours to
     * vanilla's type maps directly. Runs at mod construction, before the first resource reload resolves
     * item definitions.
     */
    private static void registerItemModelTypes() {
        ItemTintSources.ID_MAPPER.put(SlimyEyeIrisTint.ID, SlimyEyeIrisTint.MAP_CODEC);
        SpecialModelRenderers.ID_MAPPER.put(GooglyEyeItemRenderer.ID, GooglyEyeItemRenderer.Unbaked.MAP_CODEC);
    }

    private static void registerGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
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

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
