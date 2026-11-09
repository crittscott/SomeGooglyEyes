package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.config.ServerConfig;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * The server's behavior settings as clients receive them, for what clients run
 * themselves: the {@link OngoingBehavior}s on every eyed mob, and the expressions of placed mob heads and the
 * hand-held Googly Eyes. Reactive hand expressions and mob expressions are scheduled by the server ({@link ServerBehaviorScheduler}) from
 * {@link ServerConfig} directly.
 *
 * @param ambient                {@code ambientBehaviors}
 * @param minTicks               {@code ambientMinTicks}
 * @param maxTicks               {@code ambientMaxTicks}; may be below {@code minTicks}, as the server allows
 * @param pool                   the {@code ambientBehaviorPool} behaviors the server recognizes
 * @param growOnHitPercent       {@code growOnHitPercent}
 * @param swirlOnHeal            {@code swirlOnHeal}
 * @param swirlHealCooldownTicks {@code swirlHealCooldownTicks}
 * @param ongoing                the ongoing behaviors whose switches are on
 */
public record BehaviorSettings(boolean ambient, int minTicks, int maxTicks, List<EyeBehavior> pool,
    int growOnHitPercent, boolean swirlOnHeal, int swirlHealCooldownTicks,
    Set<OngoingBehavior> ongoing) {

    /** Inactive client state before server settings arrive and after disconnect. */
    public static final BehaviorSettings NONE = new BehaviorSettings(false,
        ServerConfig.AMBIENT_MIN_TICKS.defaultValue(), ServerConfig.AMBIENT_MAX_TICKS.defaultValue(), List.of(), 0,
        false, ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.defaultValue(), Set.of());

    public BehaviorSettings {
        pool = List.copyOf(pool);
        EnumSet<OngoingBehavior> copy = EnumSet.noneOf(OngoingBehavior.class);
        copy.addAll(ongoing);
        ongoing = Collections.unmodifiableSet(copy);
    }

    /** A deterministic ambient slot shared by the presentation and held-reaction eligibility. */
    public record AmbientSlot(EyeBehavior behavior, long seed, long startsAt) {
    }

    /** Filter the configured pool once when a presentation installs settings. */
    public List<EyeBehavior> ambientPool(boolean hasPartner) {
        return hasPartner ? pool : pool.stream().filter(behavior -> behavior != EyeBehavior.CROSS_EYE).toList();
    }

    public AmbientSlot ambientSlot(long seed, long slot, int slotTicks, List<EyeBehavior> eligible) {
        RandomSource random = RandomSource.create(seed ^ slot * RandomSupport.GOLDEN_RATIO_64);
        EyeBehavior behavior = eligible.get(random.nextInt(eligible.size()));
        long start = slot * slotTicks + random.nextInt(slotTicks - behavior.defaultDuration() + 1);
        return new AmbientSlot(behavior, random.nextLong(), start);
    }

    /** Shared heal reaction gate; the caller arms it only after an expression actually starts. */
    public boolean canSwirlOnHeal(long now, long readyAt) {
        return swirlOnHeal && now >= readyAt;
    }

    /**
     * A wait between ambient expressions: uniform between {@code min} and {@code max}, or {@code min} alone when
     * {@code max} doesn't exceed it. Server-scheduled mob expressions draw it here; presentations use deterministic world-time slots.
     */
    public static int drawAmbientTicks(int min, int max, RandomSource rand) {
        return max <= min ? min : min + rand.nextInt(max - min);
    }

    /** Whether {@code behavior}'s switch is on. */
    public boolean enabled(OngoingBehavior behavior) {
        return ongoing.contains(behavior);
    }

    /** The server's current settings. */
    public static BehaviorSettings fromConfig() {
        return new BehaviorSettings(ServerConfig.AMBIENT_BEHAVIORS.get(), ServerConfig.AMBIENT_MIN_TICKS.get(),
            ServerConfig.AMBIENT_MAX_TICKS.get(), ServerConfig.AMBIENT_BEHAVIOR_POOL.parsed(),
            ServerConfig.GROW_ON_HIT_PERCENT.get(), ServerConfig.SWIRL_ON_HEAL.get(),
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get(), switchedOn());
    }

    /** Reactions never interrupt the ambient expression already occupying a loose held eye. */
    public boolean handAmbientBusy(UUID holder, InteractionHand hand, long gameTime) {
        List<EyeBehavior> eligible = ambientPool(false);
        if (!ambient || eligible.isEmpty()) return false;
        int ticks = slotTicks(eligible);
        AmbientSlot slot = ambientSlot(presentationSeed(handIdentity(holder, hand)), Math.floorDiv(gameTime, ticks), ticks, eligible);
        long elapsed = gameTime - slot.startsAt();
        return elapsed >= 0 && elapsed < slot.behavior().defaultDuration();
    }

    /** A hand uses the holder identity, with an independent stable offhand seed. */
    public static UUID handIdentity(UUID holder, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? holder
        : new UUID(holder.getMostSignificantBits() ^ RandomSupport.GOLDEN_RATIO_64, holder.getLeastSignificantBits());
    }

    /**
     * The mean wait between ambient expressions, as the server's own scheduler draws it: uniform between the
     * two bounds, or the minimum alone when the maximum doesn't exceed it.
     */
    public int meanTicks() {
        return maxTicks <= minTicks ? minTicks : (minTicks + maxTicks) / 2;
    }

    /** A wait between ambient expressions under these bounds. */
    public int nextAmbientTicks(RandomSource rand) {
        return drawAmbientTicks(minTicks, maxTicks, rand);
    }

    public long nextHealReadyAt(long now) {
        return now + swirlHealCooldownTicks;
    }

    public static long presentationSeed(UUID identity) {
        return identity.getMostSignificantBits() ^ Long.rotateLeft(identity.getLeastSignificantBits(), 1);
    }

    /** Decode and validate settings written by {@link #write}. */
    public static BehaviorSettings read(FriendlyByteBuf buffer) {
        boolean ambient = buffer.readBoolean();
        int minTicks = readTicks(buffer);
        int maxTicks = readTicks(buffer);
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeBehavior.values().length) {
            throw new DecoderException("Ambient behavior pool size out of range: " + size);
        }
        List<EyeBehavior> pool = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            EyeBehavior behavior = EyeBehavior.byId(buffer.readResourceLocation());
            if (behavior == null) {
                throw new DecoderException("Unknown ambient behavior");
            }
            pool.add(behavior);
        }
        int growOnHitPercent = buffer.readVarInt();
        if (growOnHitPercent < ServerConfig.PERCENT_MIN || growOnHitPercent > ServerConfig.PERCENT_MAX) {
            throw new DecoderException("Grow-on-hit percent out of range: " + growOnHitPercent);
        }
        boolean swirlOnHeal = buffer.readBoolean();
        int swirlHealCooldownTicks = readTicks(buffer);
        int count = buffer.readVarInt();
        if (count < 0 || count > OngoingBehavior.values().length) {
            throw new DecoderException("Ongoing behavior count out of range: " + count);
        }
        EnumSet<OngoingBehavior> ongoing = EnumSet.noneOf(OngoingBehavior.class);
        for (int i = 0; i < count; i++) {
            OngoingBehavior behavior = OngoingBehavior.byId(buffer.readResourceLocation());
            if (behavior == null) {
                throw new DecoderException("Unknown ongoing behavior");
            }
            ongoing.add(behavior);
        }
        return new BehaviorSettings(ambient, minTicks, maxTicks, pool, growOnHitPercent, swirlOnHeal,
            swirlHealCooldownTicks, ongoing);
    }

    private static int readTicks(FriendlyByteBuf buffer) {
        int ticks = buffer.readVarInt();
        if (ticks < ServerConfig.TICKS_MIN || ticks > ServerConfig.TICKS_MAX) {
            throw new DecoderException("Tick interval out of range: " + ticks);
        }
        return ticks;
    }

    public boolean rollGrowOnHit(RandomSource random) {
        return ServerConfig.roll(random, growOnHitPercent);
    }

    public int slotTicks(List<EyeBehavior> eligible) {
        return Math.max(meanTicks(), eligible.stream().mapToInt(EyeBehavior::defaultDuration).max().orElse(1));
    }

    /** The ongoing behaviors whose switches are on. */
    private static Set<OngoingBehavior> switchedOn() {
        EnumSet<OngoingBehavior> on = EnumSet.noneOf(OngoingBehavior.class);
        for (OngoingBehavior behavior : OngoingBehavior.values()) {
            if (behavior.setting().get()) {
                on.add(behavior);
            }
        }
        return on;
    }

    /** Encode these settings for the client. */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(ambient);
        buffer.writeVarInt(minTicks);
        buffer.writeVarInt(maxTicks);
        buffer.writeVarInt(pool.size());
        for (EyeBehavior behavior : pool) {
            buffer.writeResourceLocation(behavior.id());
        }
        buffer.writeVarInt(growOnHitPercent);
        buffer.writeBoolean(swirlOnHeal);
        buffer.writeVarInt(swirlHealCooldownTicks);
        buffer.writeVarInt(ongoing.size());
        for (OngoingBehavior behavior : ongoing) {
            buffer.writeResourceLocation(behavior.id());
        }
    }
}
