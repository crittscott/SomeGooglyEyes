package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Keybindings for the in-world part picker. All rebindable in Options → Controls. */
public final class PickerKeys {

    /**
     * The Controls-menu category, labeled by {@code key.category.somegoogly.picker}; each loader registers it with
     * the keys.
     */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "picker"));

    // The keyboard picker is navigation + a live view only; all eye editing is done via the /sg CLI.
    // The one thing the keyboard keeps because it's far quicker than typing is choosing the mob and
    // cycling parts to find the one to attach an eye to.
    public static final KeyMapping LOCK = make("lock", InputConstants.KEY_V);
    public static final KeyMapping PART_NEXT = make("part_next", InputConstants.KEY_RBRACKET);
    public static final KeyMapping PART_PREV = make("part_prev", InputConstants.KEY_LBRACKET);
    public static final KeyMapping TOGGLE = make("toggle", InputConstants.KEY_K);

    private PickerKeys() {
    }

    private static KeyMapping make(String name, int key) {
        return new KeyMapping("key.somegoogly." + name, key, CATEGORY);
    }

}
