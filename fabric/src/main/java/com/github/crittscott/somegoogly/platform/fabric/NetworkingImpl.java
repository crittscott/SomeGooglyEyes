package com.github.crittscott.somegoogly.platform.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Fabric clientbound sends; fanout encodes once and sends through vanilla's tracking broadcasts. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        server.getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(payload));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        ((ServerChunkCache) entity.level().getChunkSource()).broadcastAndSend(entity,
                ServerPlayNetworking.createS2CPacket(payload));
    }
}
