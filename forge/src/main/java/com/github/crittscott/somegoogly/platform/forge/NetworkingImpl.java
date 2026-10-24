package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Forge clientbound sends through the mod's payload channel. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        ForgeNetworkTransport.sendToPlayer(player, payload);
    }

    public static void sendTrackingAndSelf(Entity entity, Payload payload) {
        ForgeNetworkTransport.sendTrackingAndSelf(entity, payload);
    }
}
