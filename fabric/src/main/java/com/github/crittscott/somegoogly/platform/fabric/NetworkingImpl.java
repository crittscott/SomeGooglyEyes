package com.github.crittscott.somegoogly.platform.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Fabric clientbound sends; tracking fanout uses vanilla's tracking-and-self broadcast. */
public final class NetworkingImpl {

    private NetworkingImpl() {}

    public static void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        server.getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(payload));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        ((ServerLevel) entity.level())
                .getChunkSource()
                .sendToTrackingPlayersAndSelf(
                        entity, ServerPlayNetworking.createS2CPacket(payload));
    }
}
