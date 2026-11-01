package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Disposable ambient simulations, independent for each holder's main hand and offhand. */
public final class HandEyeAnimations {

    private static final int EVICT_IDLE_TICKS = 10;
    private static final long OFFHAND_SEED = 0x9E3779B97F4A7C15L;
    private static final int SIMULATE_IDLE_TICKS = 1;

    private final Map<Key, Entry> entries = new HashMap<>();
    private long ticks;

    private record Key(UUID holder, InteractionHand hand) {
    }

    private static final class Entry {
        final ItemEyes eyes;
        long lastShown;
        final AppearanceOverride properties;

        Entry(ItemEyes eyes, AppearanceOverride properties) {
            this.eyes = eyes;
            this.properties = properties;
        }
    }

    public void clear() {
        entries.clear();
    }

    /** Stack copies and counts retain state; eye layout and appearance changes replace it. */
    @Nullable
    public ItemEyes get(UUID holder, InteractionHand hand, ItemStack stack, boolean enabled) {
        Key key = new Key(holder, hand);
        if (!enabled || !ItemEyes.supports(stack)) {
            entries.remove(key);
            return null;
        }
        AppearanceOverride properties = EyeItemProperties.get(stack);
        Entry entry = entries.get(key);
        if (entry == null || !entry.eyes.matches(stack) || !entry.properties.equals(properties)) {
            UUID seed = hand == InteractionHand.MAIN_HAND ? holder
                    : new UUID(holder.getMostSignificantBits() ^ OFFHAND_SEED, holder.getLeastSignificantBits());
            ItemEyes eyes = ItemEyes.create(seed, stack);
            if (eyes == null) {
                entries.remove(key);
                return null;
            }
            for (int part : eyes.parts) {
                for (GooglyTracker.EyeInfo pupil : eyes.eyes[part]) {
                    pupil.itemGravity = true;
                }
            }
            entry = new Entry(eyes, properties);
            entries.put(key, entry);
        }
        entry.lastShown = ticks;
        return entry.eyes;
    }

    /** Entity removal immediately releases both hands, including holders not currently visible. */
    public void remove(UUID holder) {
        entries.keySet().removeIf(key -> key.holder().equals(holder));
    }

    /** World time determines expressions; pause stops simulation and recently unseen hands expire. */
    public void tick(long gameTime, boolean simulate, BehaviorSettings settings) {
        ticks++;
        if (!simulate) {
            return;
        }
        Iterator<Entry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            long idle = ticks - entry.lastShown;
            if (idle > EVICT_IDLE_TICKS) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                entry.eyes.tick(gameTime, settings);
            }
        }
    }
}
