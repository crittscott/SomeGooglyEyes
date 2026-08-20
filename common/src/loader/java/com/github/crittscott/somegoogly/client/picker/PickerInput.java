package com.github.crittscott.somegoogly.client.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * Routes key presses to {@link PickerState} while the picker is active. Registered on the Forge
 * client bus; only the toggle key works when the picker is off.
 *
 * <p>The keyboard is navigation + a live view only: toggle the picker, lock onto a mob, and cycle
 * parts. All eye editing (position, rotation, properties, save/delete, export) is done through the
 * {@code /sg} CLI; the keyboard deliberately doesn't mutate eyes.
 */
public final class PickerInput {

    private PickerInput() {
    }

    private static boolean inCreative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    private static void message(String key, Object... args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    public static void handle(KeyMapping key) {
        // InputEvent.Key fires even while a screen is open; ignore keys when one is (e.g. typing the
        // /sg CLI in chat) so picker bindings don't fire while the player is interacting with a GUI.
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        if (key == PickerKeys.TOGGLE) {
            if (!PickerState.isActive()) {
                // The picker is a creative-mode authoring tool; don't let it turn on otherwise.
                if (!inCreative()) {
                    message("somegoogly.command.picker.toggle_requires_creative");
                    return;
                }
                PickerState.activate();
                message("somegoogly.command.picker.toggle_on");
            } else {
                PickerState.deactivate(); // also releases the frozen mob on exit
                message("somegoogly.command.picker.toggle_off");
            }
            return;
        }
        if (!PickerState.isActive()) {
            return;
        }

        if (key == PickerKeys.LOCK) {
            if (PickerState.target() != null) {
                PickerState.unlock();
                message("somegoogly.command.picker.unchose");
            } else {
                message("somegoogly.command.picker.feedback", PickerState.lockOn());
            }
        } else if (key == PickerKeys.PART_PREV) {
            PickerState.cyclePart(-1);
        } else if (key == PickerKeys.PART_NEXT) {
            PickerState.cyclePart(1);
        }
    }
}
