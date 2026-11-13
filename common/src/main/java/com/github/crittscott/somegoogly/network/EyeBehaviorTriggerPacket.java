package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

import org.jspecify.annotations.Nullable;

/**
 * Server → client: "play {@code behavior} on the eyes of entity {@code entityId}, or on the loose Googly Eye in
 * one of its hands, seeded with {@code seed}." Sent to the viewers of the entity when the server scheduler starts
 * an expression, and to a newly tracking player mid-effect with the ticks already {@code elapsed}, so every
 * viewer animates the same thing in lock-step. Every expression runs for its behavior's
 * {@link EyeBehavior#defaultDuration()}. Purely transient: the trigger is the only thing sent; the client runs the
 * animation locally.
 *
 * <p>The client retains the latest report for a known entity or hand independently of its render
 * simulation and derives its current age from elapsed simulation time, including when the simulation is rebuilt.
 */
public record EyeBehaviorTriggerPacket(int entityId, Target target, EyeBehavior behavior, long seed, int elapsed)
        implements CustomPacketPayload {

    public static final Identifier ID = SomeGooglyCommon.id("eye_behavior");
    public static final CustomPacketPayload.Type<EyeBehaviorTriggerPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeBehaviorTriggerPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeBehaviorTriggerPacket::write, EyeBehaviorTriggerPacket::read);

    /** Which eyes play the expression: those applied to the entity, or a loose Googly Eye in one of its hands. */
    public enum Target {
        ENTITY(0), MAIN_HAND(1), OFF_HAND(2);

        private final int wireId;

        Target(int wireId) {
            this.wireId = wireId;
        }

        @Nullable
        static Target byWireId(int wireId) {
            for (Target target : values()) {
                if (target.wireId == wireId) {
                    return target;
                }
            }
            return null;
        }

        public static Target forHand(InteractionHand hand) {
            return hand == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
        }

        /** The hand of a hand target. */
        public InteractionHand hand() {
            return this == MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }
    }

    private static EyeBehaviorTriggerPacket read(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        Target target = Target.byWireId(buffer.readVarInt());
        if (target == null) {
            throw new DecoderException("Unknown eye behavior target");
        }
        EyeBehavior behavior = EyeBehavior.byWireId(buffer.readVarInt());
        if (behavior == null) {
            throw new DecoderException("Unknown eye behavior");
        }
        if (target != Target.ENTITY && behavior != EyeBehavior.GROW && behavior != EyeBehavior.SWIRL) {
            throw new DecoderException("Invalid held eye reaction");
        }
        long seed = buffer.readLong();
        int elapsed = buffer.readVarInt();
        if (elapsed < 0 || elapsed > behavior.defaultDuration()) {
            throw new DecoderException("Invalid eye behavior timing");
        }
        return new EyeBehaviorTriggerPacket(entityId, target, behavior, seed, elapsed);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(target.wireId);
        buffer.writeVarInt(behavior.wireId());
        buffer.writeLong(seed);
        buffer.writeVarInt(elapsed);
    }
}
