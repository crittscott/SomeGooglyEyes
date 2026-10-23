package com.github.crittscott.somegoogly.platform.fabric;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;

/** Fabric clientbound sends; tracking fanout uses Fabric API's authoritative lookup. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buffer) {
        ServerPlayNetworking.send(player, id, buffer);
    }

    public static void sendTrackingAndSelf(Entity entity, ResourceLocation id, FriendlyByteBuf buffer) {
        Collection<ServerPlayer> recipients = new ArrayList<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer player && !recipients.contains(player)) {
            recipients.add(player);
        }
        Packet<?> packet = ServerPlayNetworking.createS2CPacket(id, buffer);
        recipients.forEach(player -> player.connection.send(packet));
    }
}
