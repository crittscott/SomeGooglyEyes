package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.picker.PickerGate;
import java.util.List;
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

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(SomeGooglyCommon.id("picker"));

    public static final KeyMapping LOCK = key("lock", GLFW.GLFW_KEY_V);
    public static final KeyMapping PART_NEXT = key("part_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeyMapping PART_PREV = key("part_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping TOGGLE = key("toggle", GLFW.GLFW_KEY_K);
    /** Every picker key, in the order their presses are handled; each loader registers exactly these. */
    public static final List<KeyMapping> KEYS = List.of(TOGGLE, LOCK, PART_PREV, PART_NEXT); // after the keys it lists

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
        for (KeyMapping key : KEYS) {
            while (key.consumeClick()) {
                handle(key);
            }
        }
    }

    /** Act on one picker key press, unless a screen is open. */
    private static void handle(KeyMapping key) {
        // A queued press is drained here, apart from when it was pressed, so a screen may have opened in between
        // (e.g. chat, to type the /sg CLI); picker actions are ignored while one is open.
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
