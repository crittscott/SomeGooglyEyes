package com.github.crittscott.somegoogly.client.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Routes key presses to {@link PickerState} while the picker is active. Registered on the Forge
 * client bus; only the toggle key works when the picker is off.
 *
 * <p>The keyboard is navigation + a live view only: toggle the picker, lock onto a mob, and cycle
 * parts. All eye editing (position, rotation, properties, save/delete, export) is done through the
 * {@code /sg} CLI; the keyboard deliberately doesn't mutate eyes.
 */
public class PickerInput {

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        // InputEvent.Key fires even while a screen is open; ignore keys when one is (e.g. typing the
        // /sg CLI in chat) so picker bindings don't fire while the player is interacting with a GUI.
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int key = event.getKey();
        int sc = event.getScanCode();
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        if (!pressed) {
            return;
        }

        if (PickerKeys.TOGGLE.matches(key, sc)) {
            if (!PickerState.active) {
                // The picker is a creative-mode authoring tool; don't let it turn on otherwise.
                if (!inCreative()) {
                    message("Picker requires creative mode.");
                    return;
                }
                PickerState.active = true;
                message("Picker ON — look at a mob and Lock (V).");
            } else {
                PickerState.active = false;
                PickerState.unlock(); // release the frozen mob on exit
                message("Picker OFF");
            }
            return;
        }
        if (!PickerState.active) {
            return;
        }

        if (PickerKeys.LOCK.matches(key, sc)) {
            if (PickerState.target() != null) {
                PickerState.unlock();
                message("Unchose (mob released).");
            } else {
                message(PickerState.lockOn());
            }
        } else if (PickerKeys.PART_PREV.matches(key, sc)) {
            PickerState.cyclePart(-1);
        } else if (PickerKeys.PART_NEXT.matches(key, sc)) {
            PickerState.cyclePart(1);
        }
    }

    private static boolean inCreative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    private static void message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Googly] " + text), true);
        }
    }
}
