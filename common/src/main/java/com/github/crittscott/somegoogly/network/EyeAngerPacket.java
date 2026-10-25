package com.github.crittscott.somegoogly.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: entity {@code entityId} became angry or calmed down. Covers anger the server keeps to
 * itself (a provoked iron golem, piglin, or zombified piglin); the client reddens the mob's pupils while
 * it is angry. Sent to a mob's trackers only when its anger starts or ends, and to a newly tracking
 * player while it is angry.
 */
public record EyeAngerPacket(int entityId, boolean angry) implements Payload {

    public static final ResourceLocation ID = NetworkHandler.EYE_ANGER;

    /** Decode a received payload. */
    public static EyeAngerPacket read(FriendlyByteBuf buffer) {
        return new EyeAngerPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(angry);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
