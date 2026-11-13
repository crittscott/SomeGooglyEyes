package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client sync of a single entity's full eye state: the {@code hasGooglyEyes} flag, the chosen
 * placement-variant roll, plus the optional per-mob appearance overrides (see {@link EyeState}). Sent on
 * start-tracking (so a newly watching player gets current state) and whenever the state is mutated
 * mid-life (so changes from shears, a Slimy Eye, or {@code /sg admin} appear immediately on every
 * tracking client). Both paths follow the entity's own spawn on the same connection, so the client always knows the entity id.
 */
public record EyeStatePacket(int entityId, EyeState.Snapshot snapshot) implements CustomPacketPayload {

    public static final Identifier ID = SomeGooglyCommon.id("eye_state");
    public static final CustomPacketPayload.Type<EyeStatePacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeStatePacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeStatePacket::write, EyeStatePacket::new);

    private EyeStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), readSnapshot(buffer));
    }

    private static EyeState.Snapshot readSnapshot(FriendlyByteBuf buffer) {
        boolean hasGooglyEyes = buffer.readBoolean();
        float variantRoll = buffer.readFloat();
        if (!validRoll(variantRoll)) {
            throw new DecoderException("Invalid eye placement variant roll");
        }
        AppearanceOverride overrides = AppearanceOverride.STREAM_CODEC.decode(buffer);
        return new EyeState.Snapshot(hasGooglyEyes, variantRoll, overrides);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(snapshot.hasEyes());
        buffer.writeFloat(snapshot.variantRoll());
        AppearanceOverride.STREAM_CODEC.encode(buffer, snapshot.properties());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean validRoll(float variantRoll) {
        return Float.isFinite(variantRoll) && variantRoll >= 0.0F && variantRoll <= 1.0F;
    }
}
