package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket.Target;
import com.github.crittscott.somegoogly.platform.Networking;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The single authority for which eye behavior a mob is playing, and when. Behaviors are cosmetic and
 * client-rendered, but the <i>schedule</i> is server-owned so the "one at a time, non-interruptable"
 * rule holds across every viewer and so server-only game events drive the same path. Triggers are the
 * ambient idle timer below plus game-event reactions (hurt/milked/sheared → grow, trade/heal → swirl, vanilla
 * affection entity events → love). A loose Googly Eye held by a watched entity also gets the holder's hurt
 * (any damage) and heal reactions, per hand ({@link #handBehavior}). A sleeping mob plays nothing: every trigger is dropped as if the mob
 * were busy, and each client derives the sleeping squint from vanilla's synced sleep state.
 *
 * <p>Anger is the one ongoing state the scheduler shares, because vanilla keeps it on the server: each
 * tick it checks every {@link #ACTIVE} mob with {@link OngoingBehavior#serverAngry} and sends an {@link EyeAngerPacket} only
 * when that changes. It never blocks a behavior; the client reddens the pupils underneath whatever plays.
 *
 * <p><b>Server-polite:</b> only mobs currently tracked by a player are considered — the tracked set is
 * maintained from {@code StartTracking}/{@code StopTracking} (so we never scan all loaded entities). A
 * tracked mob always gets a {@link #STATES} entry (needed for the {@code trackers} count regardless of
 * eye state), but {@link #serverTick} only walks {@link #ACTIVE}, the subset that can actually do
 * something: mobs known to have eyes, plus any mob mid-behavior (started via {@link #start}, including
 * the debug {@link #trigger}). A tracked-but-eyeless mob — the overwhelming majority, since the default
 * spawn chance is {@link ServerConfig#GLOBAL_PERCENT} — costs one map entry and nothing per tick unless it has a
 * recent held-eye reaction. {@link #onEyesGained} promotes a mob
 * into {@link #ACTIVE} the moment its eyes turn on (natural spawn already tracked, or a mid-life slimy
 * eye / admin toggle), called from {@link EyeState}. A promoted mob that loses its eyes leaves {@link #ACTIVE}
 * at once ({@link #onEyesLost}), or when the behavior it is playing retires.
 *
 * <p>A player never tracks themself, yet sees their own eyes in third person, so each player counts as one of
 * their own watchers from the moment they join a level ({@link #onPlayerJoined}) until they leave or are
 * replaced ({@link #onPlayerLeft}).
 *
 * <p>State is per (integrated-or-dedicated) server lifetime; {@link #clear()} is called on server stop so
 * a single-player JVM doesn't carry one world's mobs into the next. Time is the server's tick count.
 */
public final class ServerBehaviorScheduler {

    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<LivingEntity, MobState> STATES = new HashMap<>();
    // Only an idle mob leaves while tracked, so one whose behavior still needs retiring is never evicted.
    private static final Map<LivingEntity, MobState> ACTIVE = new HashMap<>();
    private static final Map<LivingEntity, Map<InteractionHand, HandState>> HANDS = new HashMap<>();
    private static boolean suspended;

    private ServerBehaviorScheduler() {
    }

    /** The last reaction of a loose Googly Eye in one hand, kept while the same item stays there. */
    private static final class HandState {
        EyeBehavior behavior;
        long healReadyAt;             // earliest server tick a heal may swirl this eye again
        final ItemStack item;
        long readyAt;                 // server tick the playing reaction ends
        long seed;
        long startedAt;

        HandState(ItemStack item) {
            this.item = item.copyWithCount(1);
        }
    }

    /** Per-mob schedule state. {@code busyUntil == 0} means idle. */
    private static final class MobState {
        EyeBehavior activeBehavior;   // the playing behavior (for mid-join catch-up)
        long activeSeed;
        boolean angry;                // the anger last sent to this mob's trackers
        int ambientCooldown;          // idle ticks remaining before the next ambient roll
        long busyUntil;               // server tick the active behavior ends (0 = idle)
        long healSwirlReadyAt;        // earliest server tick a heal may trigger another swirl (0 = ready)
        boolean self;                 // a player counted as their own watcher
        long startedAt;               // server tick the active behavior started (for mid-join catch-up)
        int trackers;                 // how many players are watching this mob, a player included
    }

    /** {@code mob}'s playing behavior for a viewer joining it partway through. */
    private static EyeBehaviorTriggerPacket catchUpPacket(LivingEntity mob, MobState state) {
        return new EyeBehaviorTriggerPacket(mob.getId(), Target.ENTITY, state.activeBehavior, state.activeSeed,
                (int) (clock(mob) - state.startedAt));
    }

    /** The server clock shared by scheduling, cooldowns, and catch-up. */
    private static long clock(LivingEntity mob) {
        return mob.level().getServer().getTickCount();
    }

    /** The reaction playing on the loose Googly Eye in {@code holder}'s {@code hand}, at its current age, or none. */
    @Nullable
    public static EyeBehaviorTriggerPacket handBehavior(LivingEntity holder, InteractionHand hand) {
        Map<InteractionHand, HandState> hands = HANDS.get(holder);
        HandState state = hands == null ? null : hands.get(hand);
        return state != null && clock(holder) < state.readyAt
                && ItemStack.isSameItemSameComponents(state.item, holder.getItemInHand(hand))
                ? handPacket(holder, hand, state) : null;
    }

    private static EyeBehaviorTriggerPacket handPacket(LivingEntity holder, InteractionHand hand, HandState state) {
        return new EyeBehaviorTriggerPacket(holder.getId(), Target.forHand(hand), state.behavior, state.seed,
                (int) (clock(holder) - state.startedAt));
    }

    /** Forget all state (server stop), so a single-player JVM doesn't bleed one world into the next. */
    public static void clear() {
        STATES.clear();
        ACTIVE.clear();
        HANDS.clear();
        suspended = false;
    }

    /**
     * The server broadcast vanilla entity event {@code eventId} for {@code entity} to its viewers. The
     * affection events (an animal entering love mode, villagers breeding, an iron golem offering a
     * flower, an ocelot gaining trust, a pet or horse being tamed) play 'love' if enabled; every other event is
     * ignored.
     */
    public static void onEntityEvent(Entity entity, byte eventId) {
        if (!(entity instanceof LivingEntity mob)) {
            return;
        }
        boolean affection = switch (eventId) {
            case EntityEvent.IN_LOVE_HEARTS, EntityEvent.LOVE_HEARTS, EntityEvent.OFFER_FLOWER,
                 EntityEvent.TAMING_SUCCEEDED, EntityEvent.TRUSTING_SUCCEEDED -> true;
            default -> false;
        };
        if (affection && ServerConfig.LOVE_ON_AFFECTION.get()) {
            react(mob, EyeBehavior.LOVE);
        }
    }

    /**
     * Promote {@code mob} into the per-tick-walked {@link #ACTIVE} set now that it's known to have eyes.
     * A no-op if nobody is currently tracking it (no {@link MobState} to promote) — {@link
     * #onStartTracking} independently checks {@link EyeState#hasEyes} for that case. Called from
     * {@link EyeState} whenever it synchronizes an eyed mob.
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
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || state == null || !EyeState.hasEyes(mob)) {
            return null;
        }
        return state;
    }

    private static int nextCooldown() {
        return BehaviorSettings.drawAmbientTicks(ServerConfig.AMBIENT_MIN_TICKS.get(),
                ServerConfig.AMBIENT_MAX_TICKS.get(), RANDOM);
    }

    /**
     * {@code mob} is about to be healed: play 'swirl' (if enabled), rate-limited per mob so regen doesn't loop it.
     * Every loader calls this ahead of the heal, on either side, since vanilla also asks a mob at full health to
     * heal. A loose Googly Eye the mob holds swirls too.
     */
    public static void onHealed(LivingEntity mob) {
        if (mob.level().isClientSide() || mob.getHealth() >= mob.getMaxHealth()) {
            return;
        }
        reactHands(mob, EyeBehavior.SWIRL);
        BehaviorSettings settings = BehaviorSettings.fromConfig();
        MobState state = eventTarget(mob);
        if (state == null || !settings.canSwirlOnHeal(clock(mob), state.healSwirlReadyAt)) {
            return;
        }
        // Arm the cooldown only on an actual start: if the mob is busy the swirl is dropped
        // (non-interruptable), and the next heal once free can still react.
        if (start(mob, state, EyeBehavior.SWIRL, RANDOM.nextLong())) {
            state.healSwirlReadyAt = settings.nextHealReadyAt(clock(mob));
        }
    }

    /**
     * {@code mob} no longer has eyes ({@link EyeState} synchronized an eyeless state). An idle mob stops being
     * walked each tick at once; one still playing a behavior leaves when that behavior retires.
     */
    public static void onEyesLost(LivingEntity mob) {
        MobState state = STATES.get(mob);
        if (state != null && state.busyUntil == 0) {
            ACTIVE.remove(mob);
            if (state.trackers <= 0) {
                STATES.remove(mob);
            }
        }
    }

    /** A player milked {@code mob}: play 'grow' (if enabled). */
    public static void onMilked(LivingEntity mob) {
        if (ServerConfig.GROW_ON_MILK.get()) {
            react(mob, EyeBehavior.GROW);
        }
    }

    /**
     * {@code player} joined a level (login, respawn, or dimension change): count them as a watcher of their own
     * eyes, once however many levels they pass through.
     */
    public static void onPlayerJoined(ServerPlayer player) {
        MobState state = stateFor(player);
        if (state.self) {
            return;
        }
        state.self = true;
        state.trackers++;
        if (EyeState.hasEyes(player)) {
            ACTIVE.putIfAbsent(player, state);
        }
    }

    /** {@code player} logged out or was replaced by a respawned copy: they no longer watch their own eyes. */
    public static void onPlayerLeft(ServerPlayer player) {
        MobState state = STATES.get(player);
        if (state != null && state.self) {
            state.self = false;
            removeWatcher(player, state);
        }
    }

    /** {@code sheep} was sheared: play 'grow' (if enabled). */
    public static void onSheared(LivingEntity sheep) {
        if (ServerConfig.GROW_ON_SHEAR.get()) {
            react(sheep, EyeBehavior.GROW);
        }
    }

    /**
     * {@code mob} took damage from {@code source}, on either side: a loose Googly Eye it holds may bulge whatever
     * hurt it, and its own eyes roll the configured chance to play 'grow' when a player did.
     */
    public static void onHurt(LivingEntity mob, DamageSource source) {
        if (mob.level().isClientSide()) {
            return;
        }
        reactHands(mob, EyeBehavior.GROW);
        if (source.getEntity() instanceof Player && ServerConfig.roll(RANDOM, ServerConfig.GROW_ON_HIT_PERCENT.get())) {
            react(mob, EyeBehavior.GROW);
        }
    }

    /**
     * A player started tracking {@code mob}. Always creates a {@link #STATES} entry (needed for the
     * {@code trackers} count regardless of eye state) and promotes into {@link #ACTIVE} immediately if
     * the mob already has eyes; a mid-life gain while already tracked comes through {@link
     * #onEyesGained} instead. Also catches the player up if the mob is already mid-effect.
     */
    public static void onStartTracking(LivingEntity mob, ServerPlayer player) {
        MobState state = stateFor(mob);
        state.trackers++;
        // Already eyed when tracking begins (natural spawn, or a slimy eye applied while untracked):
        // promote immediately. A mid-life gain while already tracked comes through onEyesGained instead.
        if (EyeState.hasEyes(mob)) {
            ACTIVE.putIfAbsent(mob, state);
        }

        if (!ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            suspended = true;
            return;
        }

        // Calm is every client's default, so only anger needs catching up. The cached delta still belongs to
        // all existing watchers.
        for (InteractionHand hand : InteractionHand.values()) {
            EyeBehaviorTriggerPacket report = handBehavior(mob, hand);
            if (report != null) {
                Networking.sendToPlayer(player, report);
            }
        }
        if (EyeState.hasEyes(mob) && OngoingBehavior.ANGER.serverAngry(mob)) {
            Networking.sendToPlayer(player, new EyeAngerPacket(mob.getId(), true));
        }
        // Mid-effect join: send just this player the active behavior with how far in it already is, so
        // they pick it up in sync rather than seeing nothing until the next one.
        if (state.busyUntil != 0) {
            Networking.sendToPlayer(player, catchUpPacket(mob, state));
        }
    }

    /** A player stopped tracking {@code mob} (including on its death/unload). Drops state when unwatched. */
    public static void onStopTracking(LivingEntity mob) {
        MobState state = STATES.get(mob);
        if (state != null) {
            removeWatcher(mob, state);
        }
    }

    /** One of {@code mob}'s watchers is gone; drop its state when none is left. */
    private static void removeWatcher(LivingEntity mob, MobState state) {
        if (--state.trackers <= 0) {
            STATES.remove(mob);
            ACTIVE.remove(mob);
        }
    }

    /** {@code villager} completed a trade with a player: play 'swirl' (if enabled). */
    public static void onTrade(LivingEntity villager) {
        if (ServerConfig.SWIRL_ON_TRADE.get()) {
            react(villager, EyeBehavior.SWIRL);
        }
    }

    /** Play {@code behavior} at its default length on {@code mob}, if it's a legitimate event target. */
    private static void react(LivingEntity mob, EyeBehavior behavior) {
        MobState state = eventTarget(mob);
        if (state != null) {
            start(mob, state, behavior, RANDOM.nextLong());
        }
    }

    /**
     * Play {@code behavior} on each loose Googly Eye {@code holder} holds, unless that eye is still reacting, is
     * playing its ambient expression, or the reaction's own gate refuses. Only a watched holder's eyes react.
     */
    private static void reactHands(LivingEntity holder, EyeBehavior behavior) {
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || holder.isSleeping()) {
            return;
        }
        if (!holder.getMainHandItem().is(ModContent.GOOGLY_EYE.get())
                && !holder.getOffhandItem().is(ModContent.GOOGLY_EYE.get())) {
            return;
        }
        MobState watched = STATES.get(holder);
        if (watched == null || watched.trackers <= 0) {
            return;
        }
        BehaviorSettings settings = BehaviorSettings.fromConfig();
        long time = clock(holder);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack item = holder.getItemInHand(hand);
            if (!item.is(ModContent.GOOGLY_EYE.get())) {
                continue;
            }
            Map<InteractionHand, HandState> hands = HANDS.computeIfAbsent(holder, key -> new EnumMap<>(InteractionHand.class));
            HandState state = hands.get(hand);
            if (state == null || !ItemStack.isSameItemSameComponents(state.item, item)) {
                state = new HandState(item);
                hands.put(hand, state);
            }
            if (time < state.readyAt || settings.handAmbientBusy(holder.getUUID(), hand, holder.level().getGameTime())
                    || behavior == EyeBehavior.SWIRL && !settings.canSwirlOnHeal(time, state.healReadyAt)
                    || behavior == EyeBehavior.GROW && !settings.rollGrowOnHit(RANDOM)) {
                continue;
            }
            state.behavior = behavior;
            state.seed = RANDOM.nextLong();
            state.startedAt = time;
            state.readyAt = time + behavior.defaultDuration();
            if (behavior == EyeBehavior.SWIRL) {
                state.healReadyAt = settings.nextHealReadyAt(time);
            }
            Networking.sendTrackingAndSelf(holder, handPacket(holder, hand, state));
        }
    }

    private static void rollAmbient(LivingEntity mob, MobState state) {
        state.ambientCooldown = nextCooldown();
        List<EyeBehavior> pool = ServerConfig.AMBIENT_BEHAVIOR_POOL.parsed();
        if (pool.isEmpty()) {
            return;
        }
        EyeBehavior behavior = pool.get(RANDOM.nextInt(pool.size()));
        start(mob, state, behavior, RANDOM.nextLong());
    }

    /**
     * Advance the scheduler one server tick: retire finished behaviors, drop states for removed or
     * unwatched-and-idle mobs, and roll a fresh ambient behavior when a watched eyed mob's cooldown
     * expires. Walks only {@link #ACTIVE} — see the class doc — so an eyeless tracked mob costs nothing
     * per tick; a mob evicted here is dropped from {@link #STATES} too, keeping the two in sync.
     */
    public static void serverTick() {
        tickHands();
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        boolean resuming = suspended && enabled;
        suspended = !enabled;
        if (resuming) {
            HANDS.forEach((holder, hands) -> hands.forEach((hand, state) -> {
                EyeBehaviorTriggerPacket packet = handBehavior(holder, hand);
                if (packet != null) {
                    Networking.sendTrackingAndSelf(holder, packet);
                }
            }));
        }
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
            // Retire a finished behavior back to idle; a mob that lost its eyes meanwhile stops being walked.
            if (state.busyUntil != 0 && clock(mob) >= state.busyUntil) {
                state.busyUntil = 0;
                state.activeBehavior = null;
                if (!EyeState.hasEyes(mob)) {
                    it.remove();
                    if (state.trackers <= 0) {
                        STATES.remove(mob);
                    }
                    continue;
                }
            }
            // Evict idle entries no player is watching. Tracked entries are removed by onStopTracking;
            // this catches states created by trigger() (the admin command) on untracked mobs, which
            // would otherwise sit in the map for the mob's whole life.
            if (state.trackers <= 0 && state.busyUntil == 0) {
                it.remove();
                STATES.remove(mob);
                continue;
            }
            if (!enabled) {
                continue;
            }
            // Watchers added while disabled received no catch-up packets; resume every current viewer.
            if (resuming && state.busyUntil != 0) {
                Networking.sendTrackingAndSelf(mob, catchUpPacket(mob, state));
            }
            // Anger changes are rare, so a packet goes out only when one happens. An eyeless mob's anger
            // stays unsent; it's picked up on the first tick after the mob gains eyes.
            // Resuming, a viewer added while disabled still needs to hear of anger that didn't change.
            boolean angry = OngoingBehavior.ANGER.serverAngry(mob);
            if ((angry != state.angry || resuming && angry) && (!angry || EyeState.hasEyes(mob))) {
                state.angry = angry;
                Networking.sendTrackingAndSelf(mob, new EyeAngerPacket(mob.getId(), angry));
            }
            // Idle + ambient enabled → count down to the next ambient roll.
            if (ambient && state.busyUntil == 0 && --state.ambientCooldown <= 0) {
                rollAmbient(mob, state);
            }
        }
    }

    private static boolean start(LivingEntity mob, MobState state, EyeBehavior behavior, long seed) {
        // Already playing something, or asleep (the client holds a squint instead): drop the trigger. A behavior
        // whose end tick has passed is over even before this tick's sweep retires it.
        long now = clock(mob);
        if (state.busyUntil != 0 && now < state.busyUntil || mob.isSleeping()) {
            return false;
        }
        state.activeBehavior = behavior;
        state.activeSeed = seed;
        state.busyUntil = now + behavior.defaultDuration();
        state.startedAt = now;
        // A started behavior needs retiring at busyUntil regardless of eye state or tracking, which
        // matters for trigger() (the admin/debug path): it can start one on a mob ACTIVE never saw yet.
        ACTIVE.putIfAbsent(mob, state);
        Networking.sendTrackingAndSelf(mob,
                new EyeBehaviorTriggerPacket(mob.getId(), Target.ENTITY, behavior, seed, 0));
        return true;
    }

    /** Forget hand reactions whose holder is gone or unwatched, whose item changed, or whose cooldowns have passed. */
    private static void tickHands() {
        HANDS.entrySet().removeIf(entry -> {
            LivingEntity holder = entry.getKey();
            if (holder.isRemoved() || !STATES.containsKey(holder)) {
                return true;
            }
            entry.getValue().entrySet().removeIf(hand -> !ItemStack.isSameItemSameComponents(hand.getValue().item,
                    holder.getItemInHand(hand.getKey()))
                    || clock(holder) >= Math.max(hand.getValue().readyAt, hand.getValue().healReadyAt));
            return entry.getValue().isEmpty();
        });
    }

    private static MobState stateFor(LivingEntity mob) {
        return STATES.computeIfAbsent(mob, m -> {
            MobState state = new MobState();
            state.ambientCooldown = nextCooldown();
            return state;
        });
    }

    /**
     * Try to start {@code behavior} on {@code mob} now, honoring "one at a time, non-interruptable":
     * dropped (returns {@code false}) if the mob is already mid-behavior or asleep. Works even for a mob with no
     * tracking state yet (creates one), so the debug command can drive any looked-at mob.
     */
    public static boolean trigger(LivingEntity mob, EyeBehavior behavior, long seed) {
        return start(mob, stateFor(mob), behavior, seed);
    }
}
