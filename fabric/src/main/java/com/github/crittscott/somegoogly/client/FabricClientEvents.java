package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

/** Fabric registration for client ticks, picker UI/input, colors, and the 3D eye item renderer. */
public final class FabricClientEvents {

    private FabricClientEvents() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(PickerKeys.LOCK);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_NEXT);
        KeyBindingHelper.registerKeyBinding(PickerKeys.PART_PREV);
        KeyBindingHelper.registerKeyBinding(PickerKeys.TOGGLE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientEyeRuntime.tick();
            EyeInspector.tick();
            consumePickerKeys();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
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
