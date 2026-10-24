package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.network.Payload;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;

/** Fabric clientbound sends; tracking fanout uses Fabric API's authoritative lookup. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        ServerPlayNetworking.send(player, payload.id(), encode(payload));
    }

    public static void sendTrackingAndSelf(Entity entity, Payload payload) {
        Collection<ServerPlayer> recipients = new ArrayList<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer player && !recipients.contains(player)) {
            recipients.add(player);
        }
        recipients.forEach(player -> sendToPlayer(player, payload));
    }

    /** A fresh buffer holding {@code payload}'s body. */
    static FriendlyByteBuf encode(Payload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        payload.write(buffer);
        return buffer;
    }
}
