package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: entity {@code entityId} became angry or calmed down. Covers anger the server
 * keeps to itself (a provoked iron golem, piglin, or zombified piglin); the client reddens the
 * mob's pupils while it is angry. Sent to a mob's trackers only when its anger starts or ends, and
 * to a newly tracking player while it is angry.
 */
public record EyeAngerPacket(int entityId, boolean angry) implements CustomPacketPayload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_anger");
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeAngerPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    EyeAngerPacket::entityId,
                    ByteBufCodecs.BOOL,
                    EyeAngerPacket::angry,
                    EyeAngerPacket::new);
    public static final CustomPacketPayload.Type<EyeAngerPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
