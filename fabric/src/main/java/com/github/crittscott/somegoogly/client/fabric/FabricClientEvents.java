package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric registration for eye render layers and their cache reset, client ticks, picker UI/input, eyed head
 * items' tooltips, the Slimy Eye color, and the 3D Googly Eye item renderer.
 */
public final class FabricClientEvents {

    private FabricClientEvents() {
    }

    /** Register the client's Fabric event callbacks. */
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) ->
                        ClientRenderLayers.install(renderer));
        if (GeckoCompat.isLoaded()) {
            FabricGeckoLayers.register();
        }
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "attachment_caches");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        ClientRenderLayers.clearCaches();
                    }
                });

        KeyBindingHelper.registerKeyBinding(PickerKeys.LOCK);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_NEXT);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_PREV);
        KeyBindingHelper.registerKeyBinding(PickerKeys.TOGGLE);

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                ClientEyeRuntime.onEntityRemoved(living);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientLifecycle.tick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ClientPlayNetworking.canSend(PickerFreezePacket.TYPE) && client.getConnection() != null) {
                client.getConnection().getConnection()
                        .disconnect(Component.translatable("somegoogly.network.required_server"));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLifecycle.onDisconnect());
        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            Minecraft minecraft = Minecraft.getInstance();
            PickerHud.render(graphics, minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
        });

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
                EyeItemProperties.appendHeadTooltip(stack, lines));

        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> EyeItemProperties.slimyEyeTint(stack, tintIndex),
                ModContent.SLIMY_EYE.get());

        GooglyEyeItemRenderer renderer = new GooglyEyeItemRenderer();
        BuiltinItemRendererRegistry.INSTANCE.register(ModContent.GOOGLY_EYE.get(), renderer::renderByItem);
    }
}
