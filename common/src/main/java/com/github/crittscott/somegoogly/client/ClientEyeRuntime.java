package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Loader-neutral owner of client eye trackers and their simulation lifecycle. */
public final class ClientEyeRuntime {

    /** Evict a tracker once it has gone unrendered for more than this many client ticks. */
    private static final int EVICT_IDLE_TICKS = 10;
    /** Simulate a tracker only while it was rendered this tick or last (freezes off-screen eyes). */
    private static final int SIMULATE_IDLE_TICKS = 1;

    private static final Map<LivingEntity, GooglyTracker> TRACKERS = new HashMap<>();
    // Anger survives render-tracker eviction but not entity unloading or client world replacement.
    private static final Set<Integer> ANGRY = new HashSet<>();
    private static int clientTicks;

    private ClientEyeRuntime() {
    }

    /** Drop every tracker; call on client disconnect and on a resource reload. */
    public static void clear() {
        TRACKERS.clear();
    }

    /** Forget every server-reported anger; call on client disconnect. */
    public static void clearAnger() {
        ANGRY.clear();
    }

    /** The client-tick counter this runtime advances once per {@link #tick(boolean)}. */
    public static int clientTicks() {
        return clientTicks;
    }

    /**
     * The tracker for this entity's current placement, creating one on first use and replacing any
     * existing tracker whose {@link HeadInfo} no longer matches (the eye array is shaped from it, so a
     * variant or age change cannot reuse the old tracker).
     */
    public static GooglyTracker get(LivingEntity living, HeadInfo helper) {
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker == null || !tracker.matches(helper)) {
            tracker = new GooglyTracker(living, helper, ANGRY.contains(living.getId()));
            TRACKERS.put(living, tracker);
        }
        return tracker;
    }

    /** Forget a removed entity's simulation and server-reported anger. */
    public static void onEntityRemoved(LivingEntity living) {
        TRACKERS.remove(living);
        ANGRY.remove(living.getId());
    }

    /** Forget simulations and anger when the client replaces or unloads its level. */
    public static void onLevelChanged() {
        clear();
        clearAnger();
    }

    /** The live tracker for this entity, or {@code null} if none exists yet (no rebuild on mismatch). */
    @Nullable
    public static GooglyTracker peek(LivingEntity living) {
        return TRACKERS.get(living);
    }

    /** Record the server-reported anger of {@code living} and pass it to its live tracker, if any. */
    public static void setAngry(LivingEntity living, boolean angry) {
        if (angry) {
            ANGRY.add(living.getId());
        } else {
            ANGRY.remove(living.getId());
        }
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker != null) {
            tracker.angry = angry;
        }
    }

    /**
     * Advance the tick counter, evict any tracker idle past {@link #EVICT_IDLE_TICKS}, and step the
     * physics of those rendered this tick or last when {@code simulate} is true. The client lifecycle
     * disables simulation while there is no level or the game is paused.
     */
    public static void tick(boolean simulate) {
        clientTicks++;
        if (!simulate) {
            return;
        }
        Iterator<Map.Entry<LivingEntity, GooglyTracker>> iterator = TRACKERS.entrySet().iterator();
        while (iterator.hasNext()) {
            GooglyTracker tracker = iterator.next().getValue();
            int idle = clientTicks - tracker.lastRenderTick;
            if (idle > EVICT_IDLE_TICKS) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                tracker.update();
            }
        }
    }
}
