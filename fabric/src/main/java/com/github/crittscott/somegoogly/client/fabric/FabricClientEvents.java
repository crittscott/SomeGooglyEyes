package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.render.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.SlimyEyeIrisTint;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Fabric registration for eye render layers and their cache reset, client ticks, picker UI/input, eyed head
 * items' tooltips, and the eye items' tint and 3D renderer types.
 */
public final class FabricClientEvents {

    private FabricClientEvents() {
    }

    /** Register the client's Fabric event callbacks. */
    public static void register() {
        // Not helper.register: it appends after a slime's outer jelly layer, hiding the eyes behind it, so the
        // shared installer inserts into the renderer's layer list itself.
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) ->
                        ClientRenderLayers.install(renderer));
        if (GeckoCompat.isLoaded()) {
            FabricGeckoLayers.register();
        }
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                SomeGooglyCommon.id("attachment_caches"),
                (ResourceManagerReloadListener) manager -> ClientRenderLayers.clearCaches());

        KeyMapping.Category.register(PickerInput.CATEGORY.id());
        PickerInput.KEYS.forEach(KeyMappingHelper::registerKeyMapping);

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            ClientEyeRuntime.onEntityRemoved(entity);
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> ClientEyeRuntime.onLevelChanged());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientLifecycle.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLifecycle.onDisconnect());
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath("somegoogly", "picker"), (graphics, partialTick) ->
                        PickerHud.render(graphics, graphics.guiWidth()));

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
                EyeItemProperties.appendHeadTooltip(stack, lines));

        // Fabric API has no registry for these; it widens vanilla's type maps for direct registration.
        ItemTintSources.ID_MAPPER.put(SlimyEyeIrisTint.ID, SlimyEyeIrisTint.MAP_CODEC);
        SpecialModelRenderers.ID_MAPPER.put(GooglyEyeItemRenderer.ID, GooglyEyeItemRenderer.Unbaked.MAP_CODEC);
    }
}
