package com.github.crittscott.somegoogly.client.picker;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** Forge registration and raw-input adapters for the shared picker client. */
public final class ForgePickerClient {

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PickerKeys.LOCK);
        event.register(PickerKeys.PART_NEXT);
        event.register(PickerKeys.PART_PREV);
        event.register(PickerKeys.TOGGLE);
    }

    public static void registerHud(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("somegoogly_picker",
                (gui, graphics, partialTick, width, height) -> PickerHud.render(graphics, width, height));
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        int key = event.getKey();
        int scanCode = event.getScanCode();
        if (PickerKeys.TOGGLE.matches(key, scanCode)) {
            PickerInput.handle(PickerKeys.TOGGLE);
        } else if (PickerKeys.LOCK.matches(key, scanCode)) {
            PickerInput.handle(PickerKeys.LOCK);
        } else if (PickerKeys.PART_PREV.matches(key, scanCode)) {
            PickerInput.handle(PickerKeys.PART_PREV);
        } else if (PickerKeys.PART_NEXT.matches(key, scanCode)) {
            PickerInput.handle(PickerKeys.PART_NEXT);
        }
    }
}
