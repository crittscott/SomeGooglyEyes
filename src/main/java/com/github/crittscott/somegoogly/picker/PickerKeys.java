package com.github.crittscott.somegoogly.picker;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Keybindings for the in-world part picker. All rebindable in Options → Controls. */
public final class PickerKeys {

    private static final String CATEGORY = "key.categories.somegoogly";

    // The keyboard picker is navigation + a live view only; all eye editing is done via the /sg CLI.
    // The one thing the keyboard keeps because it's far quicker than typing is choosing the mob and
    // cycling parts to find the one to attach an eye to.
    public static final KeyMapping TOGGLE = make("toggle", GLFW.GLFW_KEY_K);
    public static final KeyMapping LOCK = make("lock", GLFW.GLFW_KEY_V);
    public static final KeyMapping PART_PREV = make("part_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping PART_NEXT = make("part_next", GLFW.GLFW_KEY_RIGHT_BRACKET);

    private PickerKeys() {
    }

    private static KeyMapping make(String name, int key) {
        return new KeyMapping("key.somegoogly." + name, key, CATEGORY);
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
        event.register(LOCK);
        event.register(PART_PREV);
        event.register(PART_NEXT);
    }
}
