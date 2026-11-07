package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Forge clientbound sends through the mod's payload channel. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        ForgeNetworkTransport.channel.send(payload, PacketDistributor.ALL.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgeNetworkTransport.channel.send(payload, PacketDistributor.PLAYER.with(player));
    }

    public static void sendTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        ForgeNetworkTransport.channel.send(payload, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity));
    }
}
