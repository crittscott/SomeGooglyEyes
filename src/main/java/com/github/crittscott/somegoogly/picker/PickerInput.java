package com.github.crittscott.somegoogly.picker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Routes key presses to {@link PickerState} while the picker is active. Registered on the Forge
 * client bus; only the toggle key works when the picker is off. Value keys repeat on hold.
 */
public class PickerInput {

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        int key = event.getKey();
        int sc = event.getScanCode();
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        boolean held = pressed || event.getAction() == GLFW.GLFW_REPEAT;

        if (pressed && PickerKeys.TOGGLE.matches(key, sc)) {
            PickerState.active = !PickerState.active;
            if (!PickerState.active) {
                PickerState.unlock(); // release the frozen mob on exit
            }
            message(PickerState.active ? "Picker ON — look at a mob and Lock (V)." : "Picker OFF");
            return;
        }
        if (!PickerState.active) {
            return;
        }

        if (pressed && PickerKeys.LOCK.matches(key, sc)) {
            if (PickerState.target() != null) {
                PickerState.unlock();
                message("Unlocked (mob released).");
            } else {
                message(PickerState.lockOn());
            }
        } else if (pressed && PickerKeys.PART_PREV.matches(key, sc)) {
            PickerState.cyclePart(-1);
        } else if (pressed && PickerKeys.PART_NEXT.matches(key, sc)) {
            PickerState.cyclePart(1);
        } else if (pressed && PickerKeys.FIELD_PREV.matches(key, sc)) {
            PickerState.cycleField(-1);
        } else if (pressed && PickerKeys.FIELD_NEXT.matches(key, sc)) {
            PickerState.cycleField(1);
        } else if (held && PickerKeys.DECREASE.matches(key, sc)) {
            PickerState.adjust(-1);
        } else if (held && PickerKeys.INCREASE.matches(key, sc)) {
            PickerState.adjust(1);
        } else if (pressed && PickerKeys.STEP.matches(key, sc)) {
            PickerState.cycleStep(1);
            message("Step: " + PickerState.step());
        } else if (pressed && PickerKeys.COMMIT.matches(key, sc)) {
            PickerState.commit();
            message("Committed eye (" + PickerState.committedCount() + " total)");
        } else if (pressed && PickerKeys.COMMIT_MIRROR.matches(key, sc)) {
            PickerState.commitMirror();
            message("Committed mirrored pair (" + PickerState.committedCount() + " total)");
        } else if (pressed && PickerKeys.REMOVE_LAST.matches(key, sc)) {
            PickerState.removeLast();
            message("Removed last (" + PickerState.committedCount() + " total)");
        } else if (pressed && PickerKeys.GLOW.matches(key, sc)) {
            PickerState.toggleGlow();
        } else if (pressed && PickerKeys.INVIS.matches(key, sc)) {
            PickerState.toggleInvisibility();
        } else if (pressed && PickerKeys.EXPORT.matches(key, sc)) {
            message(PickerExporter.export());
        }
    }

    private static void message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Googly] " + text), true);
        }
    }
}
