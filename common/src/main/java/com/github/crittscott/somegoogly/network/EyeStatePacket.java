package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Server → client sync of a single entity's full eye state: the {@code hasGooglyEyes} flag, the chosen
 * placement-variant roll, plus the optional per-mob appearance overrides (see {@link EyeState}). Sent on
 * start-tracking (so a newly watching player gets current state) and whenever the state is mutated
 * mid-life (so changes from shears / dye / redstone appear immediately on every tracking client).
 */
public record EyeStatePacket(int entityId, UUID entityUuid, EyeState.Snapshot snapshot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EyeStatePacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.EYE_STATE);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeStatePacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeStatePacket::encode, EyeStatePacket::decode);

    public static EyeStatePacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        UUID entityUuid = buffer.readUUID();
        boolean hasGooglyEyes = buffer.readBoolean();
        float variantRoll = buffer.readFloat();
        if (!validRoll(variantRoll)) {
            throw new DecoderException("Invalid eye placement variant roll");
        }
        AppearanceOverride overrides = AppearanceOverride.STREAM_CODEC.decode(buffer);
        if (!overrides.isValid()) {
            throw new DecoderException("Invalid eye appearance color");
        }
        return new EyeStatePacket(entityId, entityUuid, new EyeState.Snapshot(hasGooglyEyes, variantRoll, overrides));
    }

    public static void encode(EyeStatePacket packet, FriendlyByteBuf buffer) {
        EyeState.Snapshot snapshot = packet.snapshot;
        if (packet.entityUuid == null || snapshot == null || !validRoll(snapshot.variantRoll())
                || snapshot.properties() == null || !snapshot.properties().isValid()) {
            throw new EncoderException("Invalid eye state packet");
        }
        buffer.writeInt(packet.entityId);
        buffer.writeUUID(packet.entityUuid);
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
