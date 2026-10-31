package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.config.ServerConfig;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The server's behavior settings as clients receive them with the eye definitions, for what clients run
 * themselves: the {@link OngoingBehavior}s on every eyed mob, and the expressions of placed mob heads and the
 * held Googly Eye. Mob expressions stay scheduled by the server ({@link ServerBehaviorScheduler}) from
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

    /** The server's built-in defaults. */
    public static final BehaviorSettings DEFAULTS = new BehaviorSettings(
            ServerConfig.AMBIENT_BEHAVIORS.defaultValue(), ServerConfig.AMBIENT_MIN_TICKS.defaultValue(),
            ServerConfig.AMBIENT_MAX_TICKS.defaultValue(), ServerConfig.defaultBehaviors(),
            ServerConfig.GROW_ON_HIT_PERCENT.defaultValue(), ServerConfig.SWIRL_ON_HEAL.defaultValue(),
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.defaultValue(), switchedOn(true));

    /** Inactive client state before server settings arrive and after disconnect. */
    public static final BehaviorSettings NONE = new BehaviorSettings(false, DEFAULTS.minTicks(), DEFAULTS.maxTicks(),
            List.of(), 0, false, DEFAULTS.swirlHealCooldownTicks(), Set.of());

    /** Largest supported behavior block, reserving space even when the server configuration later changes. */
    public static final BehaviorSettings WIRE_MAXIMUM = new BehaviorSettings(true, ServerConfig.TICKS_MAX,
            ServerConfig.TICKS_MAX, List.of(EyeBehavior.values()), ServerConfig.PERCENT_MAX, true,
            ServerConfig.TICKS_MAX, EnumSet.allOf(OngoingBehavior.class));

    public BehaviorSettings {
        pool = List.copyOf(pool);
        EnumSet<OngoingBehavior> copy = EnumSet.noneOf(OngoingBehavior.class);
        copy.addAll(ongoing);
        ongoing = Collections.unmodifiableSet(copy);
    }

    /** Whether {@code behavior}'s switch is on. */
    public boolean enabled(OngoingBehavior behavior) {
        return ongoing.contains(behavior);
    }

    /** The server's current settings. */
    public static BehaviorSettings fromConfig() {
        return new BehaviorSettings(ServerConfig.AMBIENT_BEHAVIORS.get(), ServerConfig.AMBIENT_MIN_TICKS.get(),
                ServerConfig.AMBIENT_MAX_TICKS.get(), ServerConfig.enabledBehaviors(),
                ServerConfig.GROW_ON_HIT_PERCENT.get(), ServerConfig.SWIRL_ON_HEAL.get(),
                ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get(), switchedOn(false));
    }

    /**
     * The mean wait between ambient expressions, as the server's own scheduler draws it: uniform between the
     * two bounds, or the minimum alone when the maximum doesn't exceed it.
     */
    public int meanTicks() {
        return maxTicks <= minTicks ? minTicks : (minTicks + maxTicks) / 2;
    }

    /** A wait between ambient expressions, drawn as the server's own scheduler draws it. */
    public int nextAmbientTicks(RandomSource rand) {
        return maxTicks <= minTicks ? minTicks : minTicks + rand.nextInt(maxTicks - minTicks);
    }

    /** Whether any ambient expression can play. */
    public boolean plays() {
        return ambient && !pool.isEmpty();
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

    /** The ongoing behaviors whose switches are on by default ({@code defaults}) or currently. */
    private static Set<OngoingBehavior> switchedOn(boolean defaults) {
        EnumSet<OngoingBehavior> on = EnumSet.noneOf(OngoingBehavior.class);
        for (OngoingBehavior behavior : OngoingBehavior.values()) {
            if (defaults ? behavior.setting().defaultValue() : behavior.setting().get()) {
                on.add(behavior);
            }
        }
        return on;
    }

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
