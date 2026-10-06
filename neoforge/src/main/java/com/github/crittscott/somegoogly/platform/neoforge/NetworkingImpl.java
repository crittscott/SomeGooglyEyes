package com.github.crittscott.somegoogly.platform.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge clientbound sends; tracking fanout goes through the authoritative chunk-map distributor. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendTracking(Entity entity, boolean includeSelf, CustomPacketPayload payload) {
        if (includeSelf) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        }
    }
}
