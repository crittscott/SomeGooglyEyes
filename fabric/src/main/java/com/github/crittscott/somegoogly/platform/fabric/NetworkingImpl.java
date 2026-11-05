package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.network.Payload;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Fabric clientbound sends; tracking fanout encodes once and uses the vanilla broadcast Forge's distributor does. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        ServerPlayNetworking.send(player, payload.id(), encode(payload));
    }

    public static void sendTrackingAndSelf(Entity entity, Payload payload) {
        ((ServerChunkCache) entity.level().getChunkSource()).broadcastAndSend(entity,
                ServerPlayNetworking.createS2CPacket(payload.id(), encode(payload)));
    }

    /** A fresh buffer holding {@code payload}'s body. */
    static FriendlyByteBuf encode(Payload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        payload.write(buffer);
        return buffer;
    }
}
