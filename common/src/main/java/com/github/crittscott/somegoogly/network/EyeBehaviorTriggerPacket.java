package com.github.crittscott.somegoogly.network;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: "play behavior {@code behaviorId} on entity {@code entityId} for {@code duration}
 * ticks, seeded with {@code seed}." Sent to a mob's trackers when the server scheduler starts a behavior
 * (and to a newly-tracking player mid-effect, with the full duration and the ticks already {@code elapsed}),
 * so every viewer animates the
 * same thing in lock-step. Purely transient — the trigger is the only thing sent; the client runs the
 * animation locally.
 *
 * <p>The client drops the trigger if it has no tracker for the mob yet (not currently rendering it) or if
 * a behavior is already playing — the same "one at a time, non-interruptable" rule the server enforces.
 */
public class EyeBehaviorTriggerPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EyeBehaviorTriggerPacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.EYE_BEHAVIOR);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeBehaviorTriggerPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeBehaviorTriggerPacket::write, EyeBehaviorTriggerPacket::new);

    private static final int MAX_DURATION_TICKS = 1_200;

    private final ResourceLocation behaviorId;
    private final int duration;
    private final int elapsed;
    private final int entityId;
    private final long seed;

    public EyeBehaviorTriggerPacket(int entityId, ResourceLocation behaviorId, int duration, long seed, int elapsed) {
        this.entityId = entityId;
        this.behaviorId = behaviorId;
        this.duration = duration;
        this.seed = seed;
        this.elapsed = elapsed;
    }

    private EyeBehaviorTriggerPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.behaviorId = buffer.readResourceLocation();
        this.duration = buffer.readVarInt();
        this.seed = buffer.readLong();
        this.elapsed = buffer.readVarInt();
        if (!validTiming(duration, elapsed)) {
            throw new DecoderException("Invalid eye behavior timing");
        }
    }

    private void write(FriendlyByteBuf buffer) {
        if (!validTiming(duration, elapsed)) {
            throw new EncoderException("Invalid eye behavior timing");
        }
        buffer.writeInt(entityId);
        buffer.writeResourceLocation(behaviorId);
        buffer.writeVarInt(duration);
        buffer.writeLong(seed);
        buffer.writeVarInt(elapsed);
    }

    /** The behavior to play. */
    public ResourceLocation behaviorId() {
        return behaviorId;
    }

    /** The behavior's total length in ticks. */
    public int duration() {
        return duration;
    }

    /** Ticks already played before this viewer started tracking; {@code 0} for a fresh start. */
    public int elapsed() {
        return elapsed;
    }

    /** The network id of the entity to play on. */
    public int entityId() {
        return entityId;
    }

    /** The seed that makes the behavior's randomness identical on every viewer. */
    public long seed() {
        return seed;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean validTiming(int duration, int elapsed) {
        return duration > 0 && duration <= MAX_DURATION_TICKS && elapsed >= 0 && elapsed <= duration;
    }
}
