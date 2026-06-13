package com.github.crittscott.somegoogly.picker;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Keybindings for the in-world part picker. All rebindable in Options → Controls. */
public final class PickerKeys {

    private static final String CATEGORY = "key.categories.somegoogly";

    public static final KeyMapping TOGGLE = make("toggle", GLFW.GLFW_KEY_K);
    public static final KeyMapping LOCK = make("lock", GLFW.GLFW_KEY_V);
    public static final KeyMapping PART_PREV = make("part_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping PART_NEXT = make("part_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeyMapping FIELD_PREV = make("field_prev", GLFW.GLFW_KEY_SEMICOLON);
    public static final KeyMapping FIELD_NEXT = make("field_next", GLFW.GLFW_KEY_APOSTROPHE);
    public static final KeyMapping DECREASE = make("decrease", GLFW.GLFW_KEY_MINUS);
    public static final KeyMapping INCREASE = make("increase", GLFW.GLFW_KEY_EQUAL);
    public static final KeyMapping STEP = make("step", GLFW.GLFW_KEY_BACKSLASH);
    public static final KeyMapping COMMIT = make("commit", GLFW.GLFW_KEY_ENTER);
    public static final KeyMapping COMMIT_MIRROR = make("commit_mirror", GLFW.GLFW_KEY_M);
    public static final KeyMapping REMOVE_LAST = make("remove_last", GLFW.GLFW_KEY_BACKSPACE);
    public static final KeyMapping GLOW = make("glow", GLFW.GLFW_KEY_G);
    public static final KeyMapping INVIS = make("invis", GLFW.GLFW_KEY_I);
    public static final KeyMapping EXPORT = make("export", GLFW.GLFW_KEY_P);

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
        event.register(FIELD_PREV);
        event.register(FIELD_NEXT);
        event.register(DECREASE);
        event.register(INCREASE);
        event.register(STEP);
        event.register(COMMIT);
        event.register(COMMIT_MIRROR);
        event.register(REMOVE_LAST);
        event.register(GLOW);
        event.register(INVIS);
        event.register(EXPORT);
    }
}
