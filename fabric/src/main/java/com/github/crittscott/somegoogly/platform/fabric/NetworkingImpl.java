package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport.FabricPayload;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Fabric clientbound sends; fanout encodes once and sends through vanilla's tracking broadcasts. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToAll(MinecraftServer server, Payload payload) {
        // 1.20.1 has no typed createS2CPacket overload; encode once for vanilla's broadcast.
        server.getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(payload.id(), encode(payload)));
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        ServerPlayNetworking.send(player, new FabricPayload(payload));
    }

    public static void sendTrackingAndSelf(Entity entity, Payload payload) {
        // Vanilla's tracking broadcast reuses one encoded packet for every viewer, including the entity itself.
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
