package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.SlimyEyeIrisTint;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.forge.ForgeClientConfig;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
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
        for (PlayerModelType modelType : event.getModelTypes()) {
            ClientRenderLayers.install(event.getPlayerRenderer(modelType));
            ClientRenderLayers.install(event.getMannequinRenderer(modelType));
        }
        for (EntityType entityType : BuiltInRegistries.ENTITY_TYPE) {
            EntityRenderer<?, ?> renderer = event.getEntityRenderer(entityType);
            if (renderer != null) {
                ClientRenderLayers.install(renderer);
            }
        }
    }

    /** Appended without an anchor, so the picker HUD draws after every vanilla HUD layer. */
    private static void addGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "picker"),
                (graphics, deltaTracker) -> PickerHud.render(graphics, graphics.guiWidth(), graphics.guiHeight()));
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        ClientLifecycle.tick();
    }

    private static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ClientEyeRuntime.onEntityRemoved(living);
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
        ForgeClientConfig.register(context);
        registerItemModelTypes();

        EntityRenderersEvent.AddLayers.BUS.addListener(ForgeClientBootstrap::addRendererLayers);
        AddGuiOverlayLayersEvent.BUS.addListener(ForgeClientBootstrap::addGuiLayers);
        RegisterKeyMappingsEvent.BUS.addListener(ForgeClientBootstrap::registerKeyMappings);

        RegisterClientCommandsEvent.BUS.addListener(ForgeClientBootstrap::registerClientCommands);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ForgeClientBootstrap::onClientTick);
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(ForgeClientBootstrap::onLoggingOut);
        EntityLeaveLevelEvent.BUS.addListener(ForgeClientBootstrap::onEntityLeaveLevel);
        ItemTooltipEvent.BUS.addListener(ForgeClientBootstrap::onItemTooltip);
        LevelEvent.Unload.BUS.addListener(ForgeClientBootstrap::onLevelUnload);
        if (GeckoCompat.isLoaded()) {
            ForgeGeckoLayers.register();
        }
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

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category.register(PickerKeys.CATEGORY.id());
        event.register(PickerKeys.LOCK);
        event.register(PickerKeys.PART_NEXT);
        event.register(PickerKeys.PART_PREV);
        event.register(PickerKeys.TOGGLE);
    }
}
