package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
        implements CustomPacketPayload {

    public static final ResourceLocation ID = NetworkProtocol.id("eye_behavior");
    public static final CustomPacketPayload.Type<EyeBehaviorTriggerPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeBehaviorTriggerPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeBehaviorTriggerPacket::write, EyeBehaviorTriggerPacket::read);

    private static final int MAX_DURATION_TICKS = 1_200;

    private static EyeBehaviorTriggerPacket read(FriendlyByteBuf buffer) {
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
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(behavior.wireId());
        buffer.writeVarInt(duration);
        buffer.writeLong(seed);
        buffer.writeVarInt(elapsed);
    }
}
