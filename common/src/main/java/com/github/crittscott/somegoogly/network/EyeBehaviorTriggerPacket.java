package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/**
 * Server → client: "play {@code behavior} on the eyes of entity {@code entityId}, or on the loose
 * Googly Eye in one of its hands, seeded with {@code seed}." Sent to the viewers of the entity when
 * the server scheduler starts an expression, and to a newly tracking player mid-effect with the
 * ticks already {@code elapsed}, so every viewer animates the same thing in lock-step. Every
 * expression runs for its behavior's {@link EyeBehavior#defaultDuration()}.
 *
 * <p>The client retains the latest report for a known entity or hand independently of its render
 * simulation and derives its current age from elapsed simulation time, including when the
 * simulation is rebuilt.
 */
public record EyeBehaviorTriggerPacket(
        int entityId, Target target, EyeBehavior behavior, long seed, int elapsed)
        implements CustomPacketPayload {

    /** A behavior by its {@link EyeBehavior#wireId()}; an unassigned number fails to decode. */
    private static final StreamCodec<ByteBuf, EyeBehavior> BEHAVIOR_CODEC =
            ByteBufCodecs.VAR_INT.map(
                    wireId -> {
                        EyeBehavior behavior = EyeBehavior.byWireId(wireId);
                        if (behavior == null) {
                            throw new DecoderException("Unknown eye behavior");
                        }
                        return behavior;
                    },
                    EyeBehavior::wireId);

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_behavior");
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeBehaviorTriggerPacket>
            STREAM_CODEC =
                    StreamCodec.composite(
                            ByteBufCodecs.VAR_INT,
                            EyeBehaviorTriggerPacket::entityId,
                            Target.STREAM_CODEC,
                            EyeBehaviorTriggerPacket::target,
                            BEHAVIOR_CODEC,
                            EyeBehaviorTriggerPacket::behavior,
                            ByteBufCodecs.LONG,
                            EyeBehaviorTriggerPacket::seed,
                            ByteBufCodecs.VAR_INT,
                            EyeBehaviorTriggerPacket::elapsed,
                            EyeBehaviorTriggerPacket::checked);
    public static final CustomPacketPayload.Type<EyeBehaviorTriggerPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    /**
     * Which eyes play the expression: those applied to the entity, or a loose Googly Eye in one of
     * its hands.
     */
    public enum Target {
        ENTITY(0),
        MAIN_HAND(1),
        OFF_HAND(2);

        /**
         * A target by its explicit wire id; an unknown id fails to decode. {@code ByIdMap} would
         * serve, but none of its out-of-bounds strategies rejects an unknown id.
         */
        static final StreamCodec<ByteBuf, Target> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(Target::byWireId, target -> target.wireId);

        private final int wireId;

        Target(int wireId) {
            this.wireId = wireId;
        }

        private static Target byWireId(int wireId) {
            for (Target target : values()) {
                if (target.wireId == wireId) {
                    return target;
                }
            }
            throw new DecoderException("Unknown eye behavior target");
        }

        public static Target forHand(InteractionHand hand) {
            return hand == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
        }

        /** The hand of a hand target. */
        public InteractionHand hand() {
            return this == MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }
    }

    /**
     * The decoded packet, after the checks that span fields: a held eye's reaction and the elapsed
     * time.
     */
    private static EyeBehaviorTriggerPacket checked(
            int entityId, Target target, EyeBehavior behavior, long seed, int elapsed) {
        if (target != Target.ENTITY
                && behavior != EyeBehavior.GROW
                && behavior != EyeBehavior.SWIRL) {
            throw new DecoderException("Invalid held eye reaction");
        }
        if (elapsed < 0 || elapsed > behavior.defaultDuration()) {
            throw new DecoderException("Invalid eye behavior timing");
        }
        return new EyeBehaviorTriggerPacket(entityId, target, behavior, seed, elapsed);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
