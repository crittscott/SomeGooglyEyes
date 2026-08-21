package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.EyeInspector;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModItems;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.stream.Collectors;

/** Fabric registration for client ticks, picker UI/input, colors, and the 3D eye item renderer. */
public final class FabricClientEvents {

    private static int commandTreeDebugTicks = -1;
    private static boolean loggedFirstRendererCallback;
    private static int rendererCallbacks;
    private static int rendererInstalls;

    private FabricClientEvents() {
    }

    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    rendererCallbacks++;
                    boolean installed = ClientRenderLayers.installLiving(entityType, renderer);
                    if (installed) {
                        rendererInstalls++;
                    }
                    if (!loggedFirstRendererCallback) {
                        loggedFirstRendererCallback = true;
                        SomeGooglyCommon.LOGGER.info(
                                "Fabric render debug: first living-renderer callback type={}, renderer={}, installed={}",
                                BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                                renderer.getClass().getName(), installed);
                    }
                });

        KeyBindingHelper.registerKeyBinding(PickerKeys.LOCK);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_NEXT);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_PREV);
        KeyBindingHelper.registerKeyBinding(PickerKeys.TOGGLE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            logMergedCommandTree(client);
            ClientEyeRuntime.tick();
            EyeInspector.tick();
            consumePickerKeys();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> commandTreeDebugTicks = 0);
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) ->
                ClientNetworkHandler.onEntityLoaded(entity));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            commandTreeDebugTicks = -1;
            ClientNetworkHandler.clearPendingEyeStates();
            ClientEyeConfigs.clear();
            ClientEyeRuntime.clear();
            PickerState.resetOnDisconnect();
        });
        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            Minecraft minecraft = Minecraft.getInstance();
            PickerHud.render(graphics, minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
        });

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 2
                        ? EyeItemProperties.get(stack).iris().orElse(EyeColor.BLACK).toRgb24()
                        : -1,
                ModItems.SLIMY_EYE.get());

        GooglyEyeItemRenderer renderer = new GooglyEyeItemRenderer();
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.GOOGLY_EYE.get(), renderer::renderByItem);
    }

    public static void logRendererReload(int nonLivingInstalls) {
        SomeGooglyCommon.LOGGER.info(
                "Fabric render debug: renderer reload completed; living callbacks={}, new living layers={}, new non-living layers={}",
                rendererCallbacks, rendererInstalls, nonLivingInstalls);
        rendererCallbacks = 0;
        rendererInstalls = 0;
    }

    private static void logMergedCommandTree(Minecraft client) {
        if (commandTreeDebugTicks < 0) {
            return;
        }
        commandTreeDebugTicks++;
        if (client.getConnection() == null) {
            return;
        }
        CommandNode<?> sg = client.getConnection().getCommands().getRoot().getChild("sg");
        if (sg == null && commandTreeDebugTicks < 100) {
            return;
        }
        String before = children(sg);
        FabricClientCommands.mergeSuggestions(client.getConnection().getCommands());
        sg = client.getConnection().getCommands().getRoot().getChild("sg");
        String after = children(sg);
        SomeGooglyCommon.LOGGER.info(
                "Fabric client debug: merged /sg tree before repair [{}], after repair [{}]",
                before, after);
        commandTreeDebugTicks = -1;
    }

    private static String children(CommandNode<?> node) {
        return node == null ? "missing" : node.getChildren().stream()
                .map(CommandNode::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static void consumePickerKeys() {
        while (PickerKeys.TOGGLE.consumeClick()) {
            PickerInput.handle(PickerKeys.TOGGLE);
        }
        while (PickerKeys.LOCK.consumeClick()) {
            PickerInput.handle(PickerKeys.LOCK);
        }
        while (PickerKeys.PART_PREV.consumeClick()) {
            PickerInput.handle(PickerKeys.PART_PREV);
        }
        while (PickerKeys.PART_NEXT.consumeClick()) {
            PickerInput.handle(PickerKeys.PART_NEXT);
        }
    }
}
