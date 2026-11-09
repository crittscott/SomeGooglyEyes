package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/**
 * Server-to-client transient expression on an entity's applied eyes or one specified held hand. Each scope
 * carries its behavior, seed, and elapsed time; newly tracking viewers receive the active expression at its
 * current age. Clients retain the report independently of disposable render simulations.
 */
public record EyeBehaviorTriggerPacket(int entityId, Target target, EyeBehavior behavior, long seed, int elapsed)
implements Payload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_behavior");

    public enum Target {
        ENTITY(0), MAIN_HAND(1), OFF_HAND(2);
        private final int wireId;
        Target(int wireId) { this.wireId = wireId; }
        public static Target byWireId(int wireId) {
            for (Target target : values()) if (target.wireId == wireId) return target;
            return null;
        }
        public static Target forHand(InteractionHand hand) {
            return hand == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
        }
        public InteractionHand hand() {
            return this == MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** Decode and validate a received payload. */
    public static EyeBehaviorTriggerPacket read(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        Target target = Target.byWireId(buffer.readVarInt());
        if (target == null) throw new DecoderException("Unknown eye behavior target");
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
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(target.wireId);
        buffer.writeVarInt(behavior.wireId());
        buffer.writeLong(seed);
        buffer.writeVarInt(elapsed);
    }
}
