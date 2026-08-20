package com.github.crittscott.somegoogly.platform.fabric;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;

/** Fabric entity-tracking distribution using Fabric API's authoritative lookup. */
public final class NetworkTrackingImpl {

    private NetworkTrackingImpl() {
    }

    public static void send(Entity entity, boolean includeSelf, ResourceLocation id, FriendlyByteBuf buffer) {
        Collection<ServerPlayer> recipients = new ArrayList<>(PlayerLookup.tracking(entity));
        if (includeSelf && entity instanceof ServerPlayer player && !recipients.contains(player)) {
            recipients.add(player);
        }
        NetworkManager.sendToPlayers(recipients, id, buffer);
    }
}
