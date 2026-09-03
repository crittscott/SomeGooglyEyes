package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The single authority for which eye behavior a mob is playing, and when. Behaviors are cosmetic and
 * client-rendered, but the <i>schedule</i> is server-owned so the "one at a time, non-interruptable"
 * rule holds across every viewer and so server-only game events drive the same path. Triggers are the
 * ambient idle timer below plus game-event reactions (hurt → grow, trade/heal → swirl).
 *
 * <p><b>Server-polite:</b> only mobs currently tracked by a player are considered — the tracked set is
 * maintained from {@code StartTracking}/{@code StopTracking} (so we never scan all loaded entities). A
 * tracked mob always gets a {@link #STATES} entry (needed for the {@code trackers} count regardless of
 * eye state), but {@link #serverTick} only walks {@link #ACTIVE}, the subset that can actually do
 * something: mobs known to have eyes, plus any mob mid-behavior (started via {@link #start}, including
 * the debug {@link #trigger}). A tracked-but-eyeless mob — the overwhelming majority, since the default
 * spawn chance is 5% — costs one map entry and nothing per tick. {@link #onEyesGained} promotes a mob
 * into {@link #ACTIVE} the moment its eyes turn on (natural spawn already tracked, or a mid-life slimy
 * eye / admin toggle), called from {@link EyeState}. Once promoted, a mob stays in {@link #ACTIVE} for
 * the rest of its tracked lifetime even if it later loses its eyes again (shears) — simpler and safe
 * (no risk of evicting a mob whose behavior still needs retiring), at the cost of walking a mob that
 * transiently had eyes and lost them again while still tracked, which is rare enough not to matter.
 *
 * <p>State is per (integrated-or-dedicated) server lifetime; {@link #clear()} is called on server stop so
 * a single-player JVM doesn't carry one world's mobs into the next.
 */
public final class ServerBehaviorScheduler {

    private static final Random RANDOM = new Random();
    private static final Map<LivingEntity, MobState> STATES = new HashMap<>();
    private static final Map<LivingEntity, MobState> ACTIVE = new HashMap<>();

    private static long now; // monotonic scheduler tick, advanced once per server tick

    private ServerBehaviorScheduler() {
    }

    /** Per-mob schedule state. {@code busyUntil == 0} means idle. */
    private static final class MobState {
        int activeDuration;
        ResourceLocation activeId;    // the playing behavior (for mid-join catch-up)
        long activeSeed;
        int ambientCooldown;          // idle ticks remaining before the next ambient roll
        long busyUntil;               // scheduler tick the active behavior ends (0 = idle)
        long healSwirlReadyAt;        // earliest scheduler tick a heal may trigger another swirl (0 = ready)
        long startedAt;               // scheduler tick the active behavior started (for mid-join catch-up)
        int trackers;                 // how many players are watching this mob
    }

    /** Forget all state (server stop), so a single-player JVM doesn't bleed one world into the next. */
    public static void clear() {
        STATES.clear();
        ACTIVE.clear();
        now = 0;
    }

    /**
     * Promote {@code mob} into the per-tick-walked {@link #ACTIVE} set now that it's known to have eyes.
     * A no-op if nobody is currently tracking it (no {@link MobState} to promote) — {@link
     * #onStartTracking} independently checks {@link EyeState#hasEyes} for that case. Called from
     * {@link EyeState} wherever the persisted has-eyes flag turns on.
     */
    public static void onEyesGained(LivingEntity mob) {
        MobState state = STATES.get(mob);
        if (state != null) {
            ACTIVE.putIfAbsent(mob, state);
        }
    }

    /**
     * The schedule state for {@code mob} <i>iff</i> it's a legitimate event target: a state exists for
     * every mob a player is tracking (created in {@link #onStartTracking}), so we gate on {@code hasEyes}
     * here — a tracked mob with no eyes (not yet given them, or having lost them) is not a target. Returns
     * {@code null} (caller no-ops) otherwise — so reactions never fire for off-screen or eyeless mobs.
     */
    private static MobState eventTarget(LivingEntity mob) {
        MobState state = STATES.get(mob);
        if (state == null || !EyeState.hasEyes(mob)) {
            return null;
        }
        return state;
    }

    private static int nextCooldown() {
        int min = ServerConfig.AMBIENT_MIN_TICKS.get();
        int max = ServerConfig.AMBIENT_MAX_TICKS.get();
        if (max <= min) {
            return min;
        }
        return min + RANDOM.nextInt(max - min);
    }

    /** {@code mob} was healed: play 'swirl' (if enabled), rate-limited per mob so regen doesn't loop it. */
    public static void onHealed(LivingEntity mob) {
        if (!ServerConfig.SWIRL_ON_HEAL.get()) {
            return;
        }
        MobState state = eventTarget(mob);
        if (state == null || now < state.healSwirlReadyAt) {
            return;
        }
        // Arm the cooldown only on an actual start: if the mob is busy the swirl is dropped
        // (non-interruptable), and the next heal once free can still react.
        if (start(mob, state, EyeBehaviors.SWIRL, EyeBehaviors.SWIRL.defaultDuration(), RANDOM.nextLong())) {
            state.healSwirlReadyAt = now + ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get();
        }
    }

    /** A player damaged {@code mob}: roll the configured chance to play 'grow'. */
    public static void onPlayerHurt(LivingEntity mob) {
        if (RANDOM.nextInt(ServerConfig.PERCENT_MAX) >= ServerConfig.GROW_ON_HIT_PERCENT.get()) {
            return;
        }
        MobState state = eventTarget(mob);
        if (state == null) {
            return;
        }
        start(mob, state, EyeBehaviors.GROW, EyeBehaviors.GROW.defaultDuration(), RANDOM.nextLong());
    }

    /**
     * A player started tracking {@code mob}. Always creates a {@link #STATES} entry (needed for the
     * {@code trackers} count regardless of eye state) and promotes into {@link #ACTIVE} immediately if
     * the mob already has eyes; a mid-life gain while already tracked comes through {@link
     * #onEyesGained} instead. Also catches the player up if the mob is already mid-effect.
     */
    public static void onStartTracking(LivingEntity mob, ServerPlayer player) {
        MobState state = STATES.computeIfAbsent(mob, m -> {
            MobState s = new MobState();
            s.ambientCooldown = nextCooldown();
            return s;
        });
        state.trackers++;
        // Already eyed when tracking begins (natural spawn, or a slimy eye applied while untracked):
        // promote immediately. A mid-life gain while already tracked comes through onEyesGained instead.
        if (EyeState.hasEyes(mob)) {
            ACTIVE.putIfAbsent(mob, state);
        }

        // Mid-effect join: send just this player the active behavior with how far in it already is, so
        // they pick it up in sync rather than seeing nothing until the next one.
        if (state.busyUntil != 0 && state.activeId != null) {
            int elapsed = (int) Math.max(0, now - state.startedAt);
            NetworkHandler.sendBehavior(player,
                    new EyeBehaviorTriggerPacket(mob.getId(), state.activeId,
                            state.activeDuration, state.activeSeed, elapsed));
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
            ACTIVE.remove(mob);
        }
    }

    /** {@code villager} completed a trade with a player: play 'swirl' (if enabled). */
    public static void onTrade(LivingEntity villager) {
        if (!ServerConfig.SWIRL_ON_TRADE.get()) {
            return;
        }
        MobState state = eventTarget(villager);
        if (state == null) {
            return;
        }
        start(villager, state, EyeBehaviors.SWIRL, EyeBehaviors.SWIRL.defaultDuration(), RANDOM.nextLong());
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

    /**
     * Advance the scheduler one server tick: retire finished behaviors, drop states for removed or
     * unwatched-and-idle mobs, and roll a fresh ambient behavior when a watched eyed mob's cooldown
     * expires. Walks only {@link #ACTIVE} — see the class doc — so an eyeless tracked mob costs nothing
     * per tick; a mob evicted here is dropped from {@link #STATES} too, keeping the two in sync.
     */
    public static void serverTick() {
        now++;
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        Iterator<Map.Entry<LivingEntity, MobState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, MobState> entry = it.next();
            LivingEntity mob = entry.getKey();
            MobState state = entry.getValue();

            if (mob.isRemoved()) {
                it.remove();
                STATES.remove(mob);
                continue;
            }
            // Retire a finished behavior back to idle.
            if (state.busyUntil != 0 && now >= state.busyUntil) {
                state.busyUntil = 0;
                state.activeId = null;
            }
            // Evict idle entries no player is watching. Tracked entries are removed by onStopTracking;
            // this catches states created by trigger() (the admin command) on untracked mobs, which
            // would otherwise sit in the map for the mob's whole life.
            if (state.trackers <= 0 && state.busyUntil == 0) {
                it.remove();
                STATES.remove(mob);
                continue;
            }
            // Idle + ambient enabled → count down, and only at expiry check whether the mob still has
            // eyes (a mob can be walked here after having lost them again — see the class doc).
            if (ambient && state.busyUntil == 0 && --state.ambientCooldown <= 0) {
                if (EyeState.hasEyes(mob)) {
                    rollAmbient(mob, state);
                } else {
                    state.ambientCooldown = nextCooldown();
                }
            }
        }
    }

    private static boolean start(LivingEntity mob, MobState state, EyeBehavior behavior, int duration, long seed) {
        if (state.busyUntil != 0) {
            return false; // already playing something — drop the trigger
        }
        state.activeDuration = duration;
        state.activeId = behavior.id();
        state.activeSeed = seed;
        state.busyUntil = now + duration;
        state.startedAt = now;
        // A started behavior needs retiring at busyUntil regardless of eye state or tracking, which
        // matters for trigger() (the admin/debug path): it can start one on a mob ACTIVE never saw yet.
        ACTIVE.putIfAbsent(mob, state);
        NetworkHandler.sendBehaviorTracking(mob,
                new EyeBehaviorTriggerPacket(mob.getId(), behavior.id(), duration, seed, 0));
        return true;
    }

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
}
