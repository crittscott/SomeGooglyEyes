package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.picker.PickerGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The picker's keybindings, all rebindable in Options → Controls, and the routing of their presses to
 * {@link PickerState} while the picker is active. Driven from each loader's client key-mapping and tick hooks;
 * only the toggle key works when the picker is off.
 *
 * <p>The keyboard is navigation + a live view only: toggle the picker, lock onto a mob, and cycle
 * parts. All eye editing (position, rotation, properties, save/delete, export) is done through the
 * {@code /sg} CLI; the keyboard deliberately doesn't mutate eyes.
 */
public final class PickerInput {

    private static final String CATEGORY = "key.categories.somegoogly";

    public static final KeyMapping LOCK = key("lock", GLFW.GLFW_KEY_V);
    public static final KeyMapping PART_NEXT = key("part_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeyMapping PART_PREV = key("part_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping TOGGLE = key("toggle", GLFW.GLFW_KEY_K);

    private PickerInput() {
    }

    private static void message(String key, Object... args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    private static KeyMapping key(String name, int key) {
        return new KeyMapping("key.somegoogly." + name, key, CATEGORY);
    }

    /** Drain each picker key's click queue, routing presses to {@link #handle}. Call once per client tick. */
    public static void consumePendingKeys() {
        while (TOGGLE.consumeClick()) {
            handle(TOGGLE);
        }
        while (LOCK.consumeClick()) {
            handle(LOCK);
        }
        while (PART_PREV.consumeClick()) {
            handle(PART_PREV);
        }
        while (PART_NEXT.consumeClick()) {
            handle(PART_NEXT);
        }
    }

    /** Act on one picker key press, unless a screen is open. */
    public static void handle(KeyMapping key) {
        // InputEvent.Key fires even while a screen is open; ignore keys when one is (e.g. typing the
        // /sg CLI in chat) so picker bindings don't fire while the player is interacting with a GUI.
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        if (key == TOGGLE) {
            if (!PickerState.isActive()) {
                // The server refuses picker requests from anyone else, so don't let the picker turn on.
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null || !PickerGate.mayUsePicker(player)) {
                    message("somegoogly.command.picker.not_authorized");
                    return;
                }
                PickerState.activate();
                message("somegoogly.command.picker.toggle_on", LOCK.getTranslatedKeyMessage());
            } else {
                PickerState.deactivate(); // also releases the frozen mob on exit
                message("somegoogly.command.picker.toggle_off");
            }
            return;
        }
        if (!PickerState.isActive()) {
            return;
        }

        if (key == LOCK) {
            if (PickerState.target() != null) {
                PickerState.unlock();
                message("somegoogly.command.picker.unchose");
            } else {
                message("somegoogly.command.picker.feedback", PickerState.lockOn());
            }
        } else if (key == PART_PREV) {
            PickerState.cyclePart(-1);
        } else if (key == PART_NEXT) {
            PickerState.cyclePart(1);
        }
    }
}
