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
        // InputEvent.Key fires even while a screen is open, so ignore keys when one is (e.g. typing
        // the /sg CLI in chat) — otherwise chat's Enter would trip the picker's Enter=commit binding.
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int key = event.getKey();
        int sc = event.getScanCode();
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        boolean held = pressed || event.getAction() == GLFW.GLFW_REPEAT;

        if (pressed && PickerKeys.TOGGLE.matches(key, sc)) {
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

        if (pressed && PickerKeys.LOCK.matches(key, sc)) {
            if (PickerState.target() != null) {
                PickerState.unlock();
                message("Unchose (mob released).");
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
            if (PickerState.save()) {
                message("Saved eye (" + PickerState.committedCount() + " total)");
            } else {
                message("Pick a part first (none selected).");
            }
        } else if (pressed && PickerKeys.COMMIT_MIRROR.matches(key, sc)) {
            if (PickerState.saveMirror()) {
                message("Saved mirrored pair (" + PickerState.committedCount() + " total)");
            } else {
                message("Pick a part first (none selected).");
            }
        } else if (pressed && PickerKeys.REMOVE_LAST.matches(key, sc)) {
            PickerState.deleteLast();
            message("Removed last (" + PickerState.committedCount() + " total)");
        } else if (pressed && PickerKeys.GLOW.matches(key, sc)) {
            PickerState.toggleGlow();
        } else if (pressed && PickerKeys.INVIS.matches(key, sc)) {
            PickerState.toggleInvisibility();
        } else if (pressed && PickerKeys.EXPORT.matches(key, sc)) {
            message(PickerExporter.export());
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
