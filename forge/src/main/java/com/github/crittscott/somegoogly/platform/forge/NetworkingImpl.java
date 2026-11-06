package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Forge clientbound sends through the mod's payload channel. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToAll(MinecraftServer server, Payload payload) {
        ForgeNetworkTransport.CHANNEL.send(PacketDistributor.ALL.noArg(), payload);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        ForgeNetworkTransport.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendTrackingAndSelf(Entity entity, Payload payload) {
        ForgeNetworkTransport.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), payload);
    }
}
