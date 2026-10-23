package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Forge clientbound sends through the mod's payload channel. */
public final class NetworkingImpl {

    private NetworkingImpl() {
    }

    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buffer) {
        ForgeNetworkTransport.send(PacketDistributor.PLAYER.with(() -> player), id, buffer);
    }

    public static void sendTrackingAndSelf(Entity entity, ResourceLocation id, FriendlyByteBuf buffer) {
        ForgeNetworkTransport.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), id, buffer);
    }
}
