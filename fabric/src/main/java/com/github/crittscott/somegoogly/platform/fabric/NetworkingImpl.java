package com.github.crittscott.somegoogly.platform.fabric;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

/** Fabric clientbound sends; tracking fanout uses Fabric API's authoritative lookup. */
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
        var packet = ServerPlayNetworking.createS2CPacket(payload);
        Collection<ServerPlayer> recipients = PlayerLookup.tracking(entity);
        // A server-side player without a connection has no client to synchronize.
        if (entity instanceof ServerPlayer player && player.connection != null && !recipients.contains(player)) {
            player.connection.send(packet);
        }
        recipients.forEach(player -> player.connection.send(packet));
    }
}
