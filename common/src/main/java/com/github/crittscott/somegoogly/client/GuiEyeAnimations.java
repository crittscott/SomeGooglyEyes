package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/** View-owned item simulations with a UI clock independent of world ticks and pause. */
public final class GuiEyeAnimations {

    public static final int CURSOR = 2;
    private static final int EVICT_IDLE_TICKS = 10;
    public static final int HOTBAR = 3;
    public static final Object HUD = new Object();
    public static final int POSITION = 0;
    private static final int SIMULATE_IDLE_TICKS = 1;
    public static final int SLOT = 1;

    private final Map<Key, Entry> entries = new HashMap<>();
    private long nextSeed;
    private long ticks;

    /** Coordinates are used only for generic previews; slots and cursor items use logical indexes. */
    public record Key(Object view, int kind, int x, int y, int seed) {
    }

    /** Stable identity distinguishes separately animated copies in vanilla's GUI item cache. */
    public record Presentation(ItemEyes eyes, Object identity) {
    }

    private static final class Entry {
        final ItemEyes eyes;
        final Object identity = new Object();
        long lastShown;
        final AppearanceOverride properties;

        Entry(ItemEyes eyes, AppearanceOverride properties) {
            this.eyes = eyes;
            this.properties = properties;
        }
    }

    /** Drop view-owned simulations, including ones built from old eye definitions. */
    public void clear() {
        entries.clear();
    }

    /** Request a visible presentation; unsupported or disabled items retire their former simulation. */
    @Nullable
    public Presentation get(Key key, ItemStack stack, boolean enabled) {
        if (!enabled || !ItemEyes.supports(stack)) {
            entries.remove(key);
            return null;
        }
        AppearanceOverride properties = EyeItemProperties.get(stack);
        Entry entry = entries.get(key);
        if (entry == null || !entry.eyes.matches(stack) || !entry.properties.equals(properties)) {
            ItemEyes eyes = ItemEyes.create(new UUID(0L, ++nextSeed), stack);
            if (eyes == null) {
                entries.remove(key);
                return null;
            }
            eyes.useGuiGravity();
            entry = new Entry(eyes, properties);
            entries.put(key, entry);
        }
        entry.lastShown = ticks;
        return new Presentation(entry.eyes, entry.identity);
    }

    /** Advance the UI clock even when the world is paused; hidden and departed views do no simulation. */
    public void tick(@Nullable Object activeView, BehaviorSettings settings) {
        ticks++;
        Iterator<Map.Entry<Key, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Entry> pair = iterator.next();
            Entry entry = pair.getValue();
            long idle = ticks - entry.lastShown;
            Object view = pair.getKey().view();
            if (idle > EVICT_IDLE_TICKS || view != HUD && view != activeView) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                entry.eyes.tick(ticks, settings);
            }
        }
    }
}
