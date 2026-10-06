package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Forge clientbound sends through the mod's payload channel. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgeNetworkTransport.sendToPlayer(player, payload);
    }

    public static void sendTracking(Entity entity, boolean includeSelf, CustomPacketPayload payload) {
        ForgeNetworkTransport.sendTracking(entity, includeSelf, payload);
    }
}
