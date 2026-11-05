package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Disposable ambient simulations, independent for each holder's main hand and offhand. */
public final class HandEyeAnimations {

    private static final long OFFHAND_SEED = 0x9E3779B97F4A7C15L;

    private final Map<Key, Entry> entries = new HashMap<>();
    private long ticks;

    private record Key(UUID holder, InteractionHand hand) {
    }

    private static final class Entry {
        final PresentationEyes eyes;
        final LivingEntity holder;
        long lastShown;
        final AppearanceOverride properties;

        Entry(PresentationEyes eyes, LivingEntity holder, AppearanceOverride properties) {
            this.eyes = eyes;
            this.holder = holder;
            this.properties = properties;
        }
    }

    public void clear() {
        entries.clear();
    }

    /** Stack copies and counts retain state; eye layout and appearance changes replace it. */
    @Nullable
    public PresentationEyes get(LivingEntity holder, InteractionHand hand, ItemStack stack, boolean enabled) {
        Key key = new Key(holder.getUUID(), hand);
        if (!enabled || holder.isRemoved() || !PresentationEyes.supports(stack)) {
            entries.remove(key);
            return null;
        }
        AppearanceOverride properties = EyeItemProperties.get(stack);
        Entry entry = entries.get(key);
        if (entry == null || entry.holder != holder || !entry.eyes.matches(stack) || !entry.properties.equals(properties)) {
            UUID uuid = holder.getUUID();
            UUID seed = hand == InteractionHand.MAIN_HAND ? uuid
                    : new UUID(uuid.getMostSignificantBits() ^ OFFHAND_SEED, uuid.getLeastSignificantBits());
            PresentationEyes eyes = PresentationEyes.create(seed, stack);
            if (eyes == null) {
                entries.remove(key);
                return null;
            }
            entry = new Entry(eyes, holder, properties);
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
        if (!simulate) {
            return;
        }
        ticks++;
        ClientEyeRuntime.sweep(entries, ticks, entry -> entry.lastShown,
                (key, entry) -> entry.holder.isRemoved(),
                (key, entry) -> entry.eyes.tick(gameTime, settings, entry.holder));
    }
}
