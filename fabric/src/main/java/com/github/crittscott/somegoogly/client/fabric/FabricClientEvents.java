package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.client.ClientLifecycle;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Fabric registration for client ticks, picker UI/input, colors, and the 3D eye item renderer. */
public final class FabricClientEvents {

    /** Ticks-since-join countdown for the one-shot picker command-tree merge; -1 when idle or done. */
    private static int commandTreeMergeTicks = -1;

    private FabricClientEvents() {
    }

    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) ->
                        ClientRenderLayers.installLiving(entityType, renderer));

        KeyBindingHelper.registerKeyBinding(PickerKeys.LOCK);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_NEXT);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_PREV);
        KeyBindingHelper.registerKeyBinding(PickerKeys.TOGGLE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            mergePickerCommandTree(client);
            ClientLifecycle.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ClientPlayNetworking.canSend(PickerFreezePacket.TYPE)) {
                if (client.getConnection() != null) {
                    client.getConnection().getConnection()
                            .disconnect(Component.translatable("somegoogly.network.required_server"));
                }
                return;
            }
            commandTreeMergeTicks = 0;
        });
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) ->
                ClientNetworkHandler.onEntityLoaded(entity));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            commandTreeMergeTicks = -1;
            ClientLifecycle.onDisconnect();
        });
        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            Minecraft minecraft = Minecraft.getInstance();
            PickerHud.render(graphics, minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
        });

        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> EyeItemProperties.slimyEyeTint(stack, tintIndex),
                ModItems.SLIMY_EYE.get());

        GooglyEyeItemRenderer renderer = new GooglyEyeItemRenderer();
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.GOOGLY_EYE.get(), renderer::renderByItem);
    }

    /**
     * Graft the picker's {@code /sg} verbs onto the server-supplied command dispatcher for completion
     * and help, once per join. Fabric executes a matching client command root before consulting the
     * server, so the picker nodes must also exist on the server dispatcher. The server command tree
     * arrives a few ticks after join, so this runs from the client tick until {@code /sg} appears (or a
     * short grace window elapses), then disarms.
     */
    private static void mergePickerCommandTree(Minecraft client) {
        if (commandTreeMergeTicks < 0 || client.getConnection() == null) {
            return;
        }
        boolean present = client.getConnection().getCommands().getRoot().getChild("sg") != null;
        if (!present && ++commandTreeMergeTicks < 100) {
            return;
        }
        FabricClientCommands.mergeSuggestions(client.getConnection().getCommands());
        commandTreeMergeTicks = -1;
    }
}
