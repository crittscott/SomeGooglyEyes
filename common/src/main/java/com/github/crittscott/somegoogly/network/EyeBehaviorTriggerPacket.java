package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: "play {@code behavior} on entity {@code entityId} for {@code duration}
 * ticks, seeded with {@code seed}." Sent to a mob's trackers when the server scheduler starts a behavior
 * (and to a newly-tracking player mid-effect, with the full duration and the ticks already {@code elapsed}),
 * so every viewer animates the
 * same thing in lock-step. Purely transient — the trigger is the only thing sent; the client runs the
 * animation locally.
 *
 * <p>The client retains the latest trigger for a known entity independently of its render tracker and
 * derives its current age from elapsed simulation time, including when the tracker is rebuilt.
 */
public record EyeBehaviorTriggerPacket(int entityId, EyeBehavior behavior, int duration, long seed, int elapsed)
        implements Payload {

    public static final ResourceLocation ID = NetworkHandler.EYE_BEHAVIOR;

    private static final int MAX_DURATION_TICKS = 1_200;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** Decode and validate a received payload. */
    public static EyeBehaviorTriggerPacket read(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        EyeBehavior behavior = EyeBehavior.byWireId(buffer.readVarInt());
        if (behavior == null) {
            throw new DecoderException("Unknown eye behavior");
        }
        int duration = buffer.readVarInt();
        long seed = buffer.readLong();
        int elapsed = buffer.readVarInt();
        if (!validTiming(duration, elapsed)) {
            throw new DecoderException("Invalid eye behavior timing");
        }
        return new EyeBehaviorTriggerPacket(entityId, behavior, duration, seed, elapsed);
    }

    private static boolean validTiming(int duration, int elapsed) {
        return duration > 0 && duration <= MAX_DURATION_TICKS && elapsed >= 0 && elapsed <= duration;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(behavior.wireId());
        buffer.writeVarInt(duration);
        buffer.writeLong(seed);
        buffer.writeVarInt(elapsed);
    }
}
