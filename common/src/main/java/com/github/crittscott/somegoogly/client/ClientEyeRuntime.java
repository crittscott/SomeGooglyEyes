package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Loader-neutral owner of client eye trackers and their simulation lifecycle. */
public final class ClientEyeRuntime {

    /** Evict a tracker once it has gone unrendered for more than this many client ticks. */
    private static final int EVICT_IDLE_TICKS = 10;
    /** Simulate a tracker only while it was rendered this tick or last (freezes off-screen eyes). */
    private static final int SIMULATE_IDLE_TICKS = 1;

    private static final Map<LivingEntity, GooglyTracker> TRACKERS = new HashMap<>();
    private static int clientTicks;

    private ClientEyeRuntime() {
    }

    /** Drop every tracker; call on client disconnect and on a resource reload. */
    public static void clear() {
        TRACKERS.clear();
    }

    /** The client-tick counter this runtime advances once per {@link #tick()}. */
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
            tracker = new GooglyTracker(living, helper);
            TRACKERS.put(living, tracker);
        }
        return tracker;
    }

    /** The live tracker for this entity, or {@code null} if none exists yet (no rebuild on mismatch). */
    @Nullable
    public static GooglyTracker peek(LivingEntity living) {
        return TRACKERS.get(living);
    }

    /**
     * Advance the tick counter, evict any tracker idle past {@link #EVICT_IDLE_TICKS}, and step the
     * physics of those rendered this tick or last. A no-op while there is no level or the game is paused.
     */
    public static void tick() {
        clientTicks++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
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
