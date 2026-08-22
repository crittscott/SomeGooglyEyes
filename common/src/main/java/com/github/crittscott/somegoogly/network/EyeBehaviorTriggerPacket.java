package com.github.crittscott.somegoogly.network;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: "play behavior {@code behaviorId} on entity {@code entityId} for {@code duration}
 * ticks, seeded with {@code seed}." Sent to a mob's trackers when the server scheduler starts a behavior
 * (and to a newly-tracking player mid-effect, with the remaining duration), so every viewer animates the
 * same thing in lock-step. Purely transient — the trigger is the only thing sent; the client runs the
 * animation locally.
 *
 * <p>The client drops the trigger if it has no tracker for the mob yet (not currently rendering it) or if
 * a behavior is already playing — the same "one at a time, non-interruptable" rule the server enforces.
 */
public class EyeBehaviorTriggerPacket {

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

    public static EyeBehaviorTriggerPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        ResourceLocation behaviorId = buffer.readResourceLocation();
        int duration = buffer.readVarInt();
        long seed = buffer.readLong();
        int elapsed = buffer.readVarInt();
        if (!validTiming(duration, elapsed)) {
            throw new DecoderException("Invalid eye behavior timing");
        }
        return new EyeBehaviorTriggerPacket(entityId, behaviorId, duration, seed, elapsed);
    }

    public static void encode(EyeBehaviorTriggerPacket packet, FriendlyByteBuf buffer) {
        if (!validTiming(packet.duration, packet.elapsed)) {
            throw new EncoderException("Invalid eye behavior timing");
        }
        buffer.writeInt(packet.entityId);
        buffer.writeResourceLocation(packet.behaviorId);
        buffer.writeVarInt(packet.duration);
        buffer.writeLong(packet.seed);
        buffer.writeVarInt(packet.elapsed);
    }

    public ResourceLocation behaviorId() {
        return behaviorId;
    }

    public int duration() {
        return duration;
    }

    public int elapsed() {
        return elapsed;
    }

    public int entityId() {
        return entityId;
    }

    public long seed() {
        return seed;
    }

    private static boolean validTiming(int duration, int elapsed) {
        return duration > 0 && duration <= MAX_DURATION_TICKS && elapsed >= 0 && elapsed <= duration;
    }
}
