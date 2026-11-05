package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/** View-owned item simulations with a UI clock independent of world ticks and pause. */
public final class GuiEyeAnimations {

    public static final int CURSOR = 2;
    public static final int HOTBAR = 3;
    public static final Object HUD = new Object();
    public static final int POSITION = 0;
    public static final int SLOT = 1;

    private final Map<Key, Entry> entries = new HashMap<>();
    private long nextSeed;
    private long ticks;

    /** Coordinates are used only for generic previews; slots and cursor items use logical indexes. */
    public record Key(Object view, int kind, int x, int y, int seed) {
    }

    private static final class Entry {
        final PresentationEyes eyes;
        long lastShown;

        Entry(PresentationEyes eyes) {
            this.eyes = eyes;
        }
    }

    /** Drop view-owned simulations, including ones built from old eye definitions. */
    public void clear() {
        entries.clear();
    }

    /** Request a visible presentation, replacing one built for another item; unsupported items have none. */
    @Nullable
    public PresentationEyes get(Key key, ItemStack stack) {
        Entry entry = entries.get(key);
        if (entry == null || !entry.eyes.matches(stack)) {
            PresentationEyes eyes = PresentationEyes.create(new UUID(0L, ++nextSeed), stack);
            if (eyes == null) {
                entries.remove(key);
                return null;
            }
            eyes.useGuiGravity();
            entry = new Entry(eyes);
            entries.put(key, entry);
        }
        entry.lastShown = ticks;
        return entry.eyes;
    }

    /** Advance the UI clock even when the world is paused; hidden and departed views do no simulation. */
    public void tick(@Nullable Object activeView, BehaviorSettings settings) {
        ticks++;
        ClientEyeRuntime.sweep(entries, ticks, entry -> entry.lastShown,
                (key, entry) -> key.view() != HUD && key.view() != activeView,
                (key, entry) -> entry.eyes.tick(ticks, settings));
    }
}
