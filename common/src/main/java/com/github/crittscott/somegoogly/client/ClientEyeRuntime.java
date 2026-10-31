package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Loader-neutral owner of client eye trackers, for mobs and placed heads, and their simulation lifecycle. */
public final class ClientEyeRuntime {

    /** Evict a tracker once it has gone unrendered for more than this many client ticks. */
    private static final int EVICT_IDLE_TICKS = 10;
    /** Simulate a tracker only while it was rendered this tick or last (freezes off-screen eyes). */
    private static final int SIMULATE_IDLE_TICKS = 1;

    // Anger survives render-tracker eviction but not entity unloading or client world replacement.
    private static final Set<Integer> ANGRY = new HashSet<>();
    private static final Map<LivingEntity, ReceivedBehavior> BEHAVIORS = new HashMap<>();
    /** Placed heads' eyes by position, under the same render-driven eviction and simulation as mob trackers. */
    private static final Map<BlockPos, SkullEyes> SKULLS = new HashMap<>();
    private static final Map<LivingEntity, GooglyTracker> TRACKERS = new HashMap<>();
    private static int clientTicks;
    private static long simulationTicks;

    private ClientEyeRuntime() {
    }

    /** Entity-lifetime expression timing, independent of any disposable render tracker. */
    record ReceivedBehavior(EyeBehavior behavior, int duration, long seed, long startedAt) {
    }

    /** Drop render simulations while retaining entity-lifetime reports. */
    public static void clear() {
        SKULLS.clear();
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
            tracker = new GooglyTracker(living, helper, ANGRY.contains(living.getId()), ClientEyeConfigs.behaviors());
            TRACKERS.put(living, tracker);
        }
        synchronizeBehavior(living, tracker, 0);
        return tracker;
    }

    /** Forget a removed entity's simulation and server-reported anger. */
    public static void onEntityRemoved(LivingEntity living) {
        TRACKERS.remove(living);
        ANGRY.remove(living.getId());
        BEHAVIORS.remove(living);
    }

    /** Forget simulations and anger when the client replaces or unloads its level. */
    public static void onLevelChanged() {
        clear();
        clearAnger();
        BEHAVIORS.clear();
        simulationTicks = 0;
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

    /** Retain the latest server expression even before this known entity has rendered. */
    public static void setBehavior(LivingEntity living, EyeBehavior behavior, int duration, long seed, int elapsed) {
        BEHAVIORS.put(living, new ReceivedBehavior(behavior, duration, seed, simulationTicks - elapsed));
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker != null) {
            synchronizeBehavior(living, tracker, 0);
        }
    }

    /**
     * The eyes of the eyed head {@code skull}, a head of {@code entity}'s kind, creating them on first use and
     * replacing any built for another kind of head at the same position; {@code null} when the client's
     * definitions give it nothing to draw.
     */
    @Nullable
    public static SkullEyes skullEyes(SkullBlockEntity skull, ResourceLocation entity) {
        SkullEyes eyes = SKULLS.get(skull.getBlockPos());
        if (eyes == null || !eyes.entity.equals(entity)) {
            eyes = SkullEyes.create(skull.getBlockPos(), entity, SkullEyeState.snapshot(skull));
            if (eyes == null) {
                SKULLS.remove(skull.getBlockPos());
                return null;
            }
            SKULLS.put(skull.getBlockPos().immutable(), eyes);
        }
        return eyes;
    }

    private static void synchronizeBehavior(LivingEntity living, GooglyTracker tracker, int ageOffset) {
        ReceivedBehavior received = BEHAVIORS.get(living);
        if (received == null) {
            return;
        }
        long elapsed = simulationTicks - received.startedAt();
        if (elapsed >= received.duration()) {
            BEHAVIORS.remove(living);
            tracker.active = null;
            tracker.receivedBehavior = null;
            return;
        }
        if (tracker.receivedBehavior != received) {
            tracker.active = null;
            tracker.startBehavior(received.behavior(), received.duration(), received.seed(), (int) elapsed);
            tracker.receivedBehavior = received;
        }
        if (tracker.active != null) {
            tracker.active.age = (int) elapsed + ageOffset;
        }
    }

    /**
     * Advance the tick counter, evict any tracker idle past {@link #EVICT_IDLE_TICKS}, and step the
     * physics of those rendered this tick or last when {@code simulate} is true. The client lifecycle
     * disables simulation while there is no level or the game is paused. A head's eyes are also dropped
     * when its eye state has changed, to be rebuilt from the new state when it next renders.
     */
    public static void tick(boolean simulate) {
        clientTicks++;
        if (!simulate) {
            return;
        }
        tickBehaviors();
        ClientLevel level = Minecraft.getInstance().level;
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        Iterator<Map.Entry<BlockPos, SkullEyes>> skulls = SKULLS.entrySet().iterator();
        while (skulls.hasNext()) {
            Map.Entry<BlockPos, SkullEyes> entry = skulls.next();
            SkullEyes eyes = entry.getValue();
            int idle = clientTicks - eyes.lastRenderTick;
            if (idle > EVICT_IDLE_TICKS || level == null
                    || !(level.getBlockEntity(entry.getKey()) instanceof SkullBlockEntity skull)
                    || !SkullEyeState.snapshot(skull).equals(eyes.snapshot)) {
                skulls.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                eyes.tick(level.getGameTime(), behaviors);
            }
        }
        Iterator<Map.Entry<LivingEntity, GooglyTracker>> iterator = TRACKERS.entrySet().iterator();
        while (iterator.hasNext()) {
            GooglyTracker tracker = iterator.next().getValue();
            int idle = clientTicks - tracker.lastRenderTick;
            if (idle > EVICT_IDLE_TICKS) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                synchronizeBehavior(tracker.parent, tracker, -1);
                tracker.update(behaviors);
            }
        }
    }

    /** Advance received expression time once per unpaused simulation tick, even for unrendered entities. */
    public static void tickBehaviors() {
        simulationTicks++;
        BEHAVIORS.entrySet().removeIf(entry -> {
            boolean expired = simulationTicks - entry.getValue().startedAt() >= entry.getValue().duration();
            if (expired) {
                GooglyTracker tracker = TRACKERS.get(entry.getKey());
                if (tracker != null) {
                    tracker.active = null;
                    tracker.receivedBehavior = null;
                }
            }
            return expired;
        });
    }
}
