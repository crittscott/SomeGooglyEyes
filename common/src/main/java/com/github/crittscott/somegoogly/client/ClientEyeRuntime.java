package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToLongFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Loader-neutral owner of mob, boat, placed-head, and framed-item eye simulations and their lifecycle. */
public final class ClientEyeRuntime {

    /** Evict a simulation once it has gone unshown for more than this many of its cache's ticks. */
    private static final int EVICT_IDLE_TICKS = 10;
    /** Simulate only what was shown this tick or last (freezes off-screen eyes). */
    private static final int SIMULATE_IDLE_TICKS = 1;

    // Anger survives render-tracker eviction but not entity unloading or client world replacement.
    private static final Set<Integer> ANGRY = new HashSet<>();
    private static final Map<LivingEntity, ReceivedBehavior> BEHAVIORS = new HashMap<>();
    private static final BoatEyeAnimations BOATS = new BoatEyeAnimations();
    private static final Map<ItemFrame, PresentationEyes> FRAMES = new HashMap<>();
    /** Placed heads' eyes by position, under the same render-driven eviction and simulation as mob trackers. */
    private static final Map<BlockPos, PresentationEyes> SKULLS = new HashMap<>();
    private static final Map<LivingEntity, GooglyTracker> TRACKERS = new HashMap<>();
    private static int clientTicks;
    private static long simulationTicks;

    private ClientEyeRuntime() {
    }

    /** Entity-lifetime expression timing, independent of any disposable render tracker. */
    record ReceivedBehavior(EyeBehavior behavior, int duration, long seed, long startedAt) {
    }

    /** The current boat layout, rebuilt only when its state, geometry, or definition changes. */
    @Nullable
    public static PresentationEyes boatEyes(Boat boat) {
        return BOATS.get(boat);
    }

    /** Drop render simulations while retaining entity-lifetime reports. */
    public static void clear() {
        GuiItemEyes.clear();
        HandItemEyes.clear();
        BOATS.clear();
        FRAMES.clear();
        SKULLS.clear();
        TRACKERS.clear();
    }

    /** Forget every server-reported anger; call on client disconnect. */
    public static void clearAnger() {
        ANGRY.clear();
    }

    /** The client-tick counter this runtime advances once per {@link #tick(Level, boolean)}. */
    public static int clientTicks() {
        return clientTicks;
    }

    /** The rendered frame's eye, or none while its item or the global switches disallow animation. */
    @Nullable
    public static PresentationEyes frameEye(ItemFrame frame) {
        if (!ClientEyeConfigs.eyesVisible() || frame.isRemoved() || !PresentationEyes.supports(frame.getItem())) {
            FRAMES.remove(frame);
            return null;
        }
        PresentationEyes eye = FRAMES.get(frame);
        if (eye == null || !eye.matches(frame.getItem())) {
            eye = PresentationEyes.create(frame.getUUID(), frame.getItem());
            if (eye == null) {
                FRAMES.remove(frame);
                return null;
            }
            FRAMES.put(frame, eye);
        }
        eye.markRendered(clientTicks);
        return eye;
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
            synchronizeBehavior(living, tracker, 0);
        }
        return tracker;
    }

    /** A boat's eye state arrived from the server; a differing state replaces its simulation. */
    public static void onBoatState(Entity boat, EyeState.Snapshot snapshot) {
        BOATS.onStateChanged(boat, snapshot);
    }

    /** Forget a removed entity's simulation and server-reported anger. */
    public static void onEntityRemoved(Entity entity) {
        BOATS.remove(entity);
        HandItemEyes.remove(entity.getUUID());
        TRACKERS.remove(entity);
        ANGRY.remove(entity.getId());
        BEHAVIORS.remove(entity);
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
     * replacing any built for another kind of head at the same position or from another state; {@code null}
     * when the client's definitions give it nothing to draw. A load with unchanged state keeps the pupils.
     */
    @Nullable
    public static PresentationEyes skullEyes(BlockEntity skull, ResourceLocation entity) {
        PresentationEyes eyes = SKULLS.get(skull.getBlockPos());
        CompoundTag data = SkullEyeState.data(skull);
        if (eyes != null && eyes.headData != data) {
            if (SkullEyeState.snapshot(skull).equals(eyes.snapshot)) {
                eyes.headData = data;
            } else {
                eyes = null;
            }
        }
        if (eyes == null || !eyes.entity.equals(entity)) {
            var snapshot = SkullEyeState.snapshot(skull);
            var layout = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
            int[] parts = SkullEyeState.headParts(entity, layout);
            if (parts.length == 0) {
                SKULLS.remove(skull.getBlockPos());
                return null;
            }
            eyes = PresentationEyes.placedHead(Mth.getSeed(skull.getBlockPos()), entity, snapshot, layout, parts);
            eyes.headData = data;
            SKULLS.put(skull.getBlockPos().immutable(), eyes);
        }
        return eyes;
    }

    /**
     * The render-driven lifecycle every eye cache shares, on that cache's own clock: an entry unshown past
     * {@link #EVICT_IDLE_TICKS} or now {@code stale} is removed, and one shown within
     * {@link #SIMULATE_IDLE_TICKS} steps.
     */
    static <K, V> void sweep(Map<K, V> entries, long now, ToLongFunction<V> lastShown,
                             BiPredicate<K, V> stale, BiConsumer<K, V> step) {
        Iterator<Map.Entry<K, V>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            long idle = now - lastShown.applyAsLong(entry.getValue());
            if (idle > EVICT_IDLE_TICKS || stale.test(entry.getKey(), entry.getValue())) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                step.accept(entry.getKey(), entry.getValue());
            }
        }
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
     * disables simulation while there is no level or the game is paused.
     */
    public static void tick(@Nullable Level level, boolean simulate) {
        clientTicks++;
        if (!simulate) {
            return;
        }
        tickBehaviors();
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        if (!ClientEyeConfigs.eyesVisible()) {
            FRAMES.clear();
        }
        BOATS.tick(level.getGameTime(), behaviors);
        sweep(FRAMES, clientTicks, eye -> eye.lastRenderTick,
                (frame, eye) -> frame.isRemoved(),
                (frame, eye) -> eye.tick(level.getGameTime(), behaviors));
        sweep(SKULLS, clientTicks, eyes -> eyes.lastRenderTick, (pos, eyes) -> false,
                (pos, eyes) -> eyes.tick(level.getGameTime(), behaviors));
        sweep(TRACKERS, clientTicks, tracker -> tracker.lastRenderTick, (living, tracker) -> false,
                (living, tracker) -> {
                    synchronizeBehavior(tracker.parent, tracker, -1);
                    tracker.update(behaviors);
                });
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
