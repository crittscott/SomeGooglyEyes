package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.config.ServerConfig;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * The server's ambient-expression settings as clients receive them with the eye definitions, for the
 * expressions clients schedule themselves: those of placed mob heads. Mob expressions stay scheduled by the
 * server ({@link ServerBehaviorScheduler}) from {@link ServerConfig} directly.
 *
 * @param enabled  {@code ambientBehaviors}
 * @param minTicks {@code ambientMinTicks}
 * @param maxTicks {@code ambientMaxTicks}; may be below {@code minTicks}, as the server allows
 * @param pool     the {@code ambientBehaviorPool} behaviors the server recognizes
 */
public record AmbientSettings(boolean enabled, int minTicks, int maxTicks, List<EyeBehavior> pool) {

    /** Nothing plays; what a client holds before its first sync. */
    public static final AmbientSettings NONE = new AmbientSettings(false, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MIN,
            List.of());

    /** The server's current settings. */
    public static AmbientSettings fromConfig() {
        return new AmbientSettings(ServerConfig.AMBIENT_BEHAVIORS.get(), ServerConfig.AMBIENT_MIN_TICKS.get(),
                ServerConfig.AMBIENT_MAX_TICKS.get(), List.copyOf(ServerConfig.enabledBehaviors()));
    }

    /**
     * The mean wait between ambient expressions, as the server's own scheduler draws it: uniform between the
     * two bounds, or the minimum alone when the maximum doesn't exceed it.
     */
    public int meanTicks() {
        return maxTicks <= minTicks ? minTicks : (minTicks + maxTicks) / 2;
    }

    /** Whether any expression can play. */
    public boolean plays() {
        return enabled && !pool.isEmpty();
    }

    /** Decode and validate settings written by {@link #write}. */
    public static AmbientSettings read(FriendlyByteBuf buffer) {
        boolean enabled = buffer.readBoolean();
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
        return new AmbientSettings(enabled, minTicks, maxTicks, List.copyOf(pool));
    }

    private static int readTicks(FriendlyByteBuf buffer) {
        int ticks = buffer.readVarInt();
        if (ticks < ServerConfig.TICKS_MIN || ticks > ServerConfig.TICKS_MAX) {
            throw new DecoderException("Ambient interval out of range: " + ticks);
        }
        return ticks;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(enabled);
        buffer.writeVarInt(minTicks);
        buffer.writeVarInt(maxTicks);
        buffer.writeVarInt(pool.size());
        for (EyeBehavior behavior : pool) {
            buffer.writeResourceLocation(behavior.id());
        }
    }
}
