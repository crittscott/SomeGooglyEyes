package com.github.crittscott.somegoogly.behavior;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.state.EyeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The single authority for which eye behavior a mob is playing, and when. Behaviors are cosmetic and
 * client-rendered, but the <i>schedule</i> is server-owned so the "one at a time, non-interruptable"
 * rule holds across every viewer and so server-only game events (a villager levelling up, …) can drive
 * the same path later. For now the only trigger is the ambient idle timer below.
 *
 * <p><b>Server-polite:</b> only mobs that have eyes <i>and</i> are currently tracked by a player are
 * considered. The tracked set is maintained from {@code StartTracking}/{@code StopTracking} (so we never
 * scan all loaded entities), and each tick only walks that small map. All state is transient — nothing
 * is saved; a reload starts fresh.
 *
 * <p>State is per (integrated-or-dedicated) server lifetime; {@link #clear()} is called on server stop so
 * a single-player JVM doesn't carry one world's mobs into the next.
 */
public final class ServerBehaviorScheduler {

    /** Per-mob schedule state. {@code busyUntil == 0} means idle. */
    private static final class MobState {
        int trackers;                 // how many players are watching this mob
        long busyUntil;               // scheduler tick the active behavior ends (0 = idle)
        long startedAt;               // scheduler tick the active behavior started (for mid-join catch-up)
        ResourceLocation activeId;    // the playing behavior (for mid-join catch-up)
        int activeDuration;
        long activeSeed;
        int ambientCooldown;          // idle ticks remaining before the next ambient roll
    }

    private static final Map<LivingEntity, MobState> STATES = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static long now; // monotonic scheduler tick, advanced once per server tick

    private ServerBehaviorScheduler() {
    }

    // --- tracking bookkeeping (called from ServerEventHandler) --------------------------------

    /** A player started tracking {@code mob}. Registers it (if eyed) and catches the player up mid-effect. */
    public static void onStartTracking(LivingEntity mob, ServerPlayer player) {
        if (!EyeState.hasEyes(mob)) {
            return;
        }
        MobState state = STATES.computeIfAbsent(mob, m -> {
            MobState s = new MobState();
            s.ambientCooldown = nextCooldown();
            return s;
        });
        state.trackers++;

        // Mid-effect join: send just this player the active behavior with how far in it already is, so
        // they pick it up in sync rather than seeing nothing until the next one.
        if (state.busyUntil != 0 && state.activeId != null) {
            int elapsed = (int) Math.max(0, now - state.startedAt);
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new EyeBehaviorTriggerPacket(mob.getId(), state.activeId, state.activeDuration, state.activeSeed, elapsed));
        }
    }

    /** A player stopped tracking {@code mob} (including on its death/unload). Drops state when unwatched. */
    public static void onStopTracking(LivingEntity mob) {
        MobState state = STATES.get(mob);
        if (state == null) {
            return;
        }
        if (--state.trackers <= 0) {
            STATES.remove(mob);
        }
    }

    // --- per-tick driver (called from ServerEventHandler on ServerTickEvent END) --------------

    public static void serverTick() {
        now++;
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        Iterator<Map.Entry<LivingEntity, MobState>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, MobState> entry = it.next();
            LivingEntity mob = entry.getKey();
            MobState state = entry.getValue();

            if (mob.isRemoved()) {
                it.remove();
                continue;
            }
            // Retire a finished behavior back to idle.
            if (state.busyUntil != 0 && now >= state.busyUntil) {
                state.busyUntil = 0;
                state.activeId = null;
            }
            // Idle + ambient enabled + still eyed → count down and roll.
            if (ambient && state.busyUntil == 0 && EyeState.hasEyes(mob) && --state.ambientCooldown <= 0) {
                rollAmbient(mob, state);
            }
        }
    }

    private static void rollAmbient(LivingEntity mob, MobState state) {
        state.ambientCooldown = nextCooldown();
        List<EyeBehavior> pool = ServerConfig.enabledBehaviors();
        if (pool.isEmpty()) {
            return;
        }
        EyeBehavior behavior = pool.get(RANDOM.nextInt(pool.size()));
        start(mob, state, behavior, behavior.defaultDuration(), RANDOM.nextLong());
    }

    // --- manual trigger (debug command, and the seam future game events will call) -------------

    /**
     * Try to start {@code behavior} on {@code mob} now, honoring "one at a time, non-interruptable":
     * dropped (returns {@code false}) if the mob is already mid-behavior. Works even for a mob with no
     * tracking state yet (creates one), so the debug command can drive any looked-at mob.
     */
    public static boolean trigger(LivingEntity mob, EyeBehavior behavior, int duration, long seed) {
        MobState state = STATES.computeIfAbsent(mob, m -> {
            MobState s = new MobState();
            s.ambientCooldown = nextCooldown();
            return s;
        });
        return start(mob, state, behavior, duration, seed);
    }

    private static boolean start(LivingEntity mob, MobState state, EyeBehavior behavior, int duration, long seed) {
        if (state.busyUntil != 0) {
            return false; // already playing something — drop the trigger
        }
        state.busyUntil = now + duration;
        state.startedAt = now;
        state.activeId = behavior.id();
        state.activeDuration = duration;
        state.activeSeed = seed;
        NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob),
                new EyeBehaviorTriggerPacket(mob.getId(), behavior.id(), duration, seed, 0));
        return true;
    }

    private static int nextCooldown() {
        int min = ServerConfig.AMBIENT_MIN_TICKS.get();
        int max = ServerConfig.AMBIENT_MAX_TICKS.get();
        if (max <= min) {
            return min;
        }
        return min + RANDOM.nextInt(max - min);
    }

    /** Forget all state (server stop), so a single-player JVM doesn't bleed one world into the next. */
    public static void clear() {
        STATES.clear();
        now = 0;
    }
}
