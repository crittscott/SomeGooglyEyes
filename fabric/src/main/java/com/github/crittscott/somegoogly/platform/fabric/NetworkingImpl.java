package com.github.crittscott.somegoogly.platform.fabric;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;

/** Fabric clientbound sends; tracking fanout uses Fabric API's authoritative lookup. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        Collection<ServerPlayer> recipients = new ArrayList<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer player && !recipients.contains(player)) {
            recipients.add(player);
        }
        recipients.forEach(player -> ServerPlayNetworking.send(player, payload));
    }
}
