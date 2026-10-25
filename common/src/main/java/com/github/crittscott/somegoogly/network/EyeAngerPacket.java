package com.github.crittscott.somegoogly.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: entity {@code entityId} became angry or calmed down. Covers anger the server keeps to
 * itself (a provoked iron golem, piglin, or zombified piglin); the client reddens the mob's pupils while
 * it is angry. Sent to a mob's trackers only when its anger starts or ends, and to a newly tracking
 * player while it is angry.
 */
public record EyeAngerPacket(int entityId, boolean angry) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EyeAngerPacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.EYE_ANGER);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeAngerPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeAngerPacket::write, EyeAngerPacket::new);

    private EyeAngerPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBoolean());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(angry);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
