package com.github.crittscott.somegoogly.platform.forge;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Forge entity-tracking distribution using its chunk-map-backed packet target. */
public final class NetworkTrackingImpl {

    private NetworkTrackingImpl() {
    }

    public static void send(Entity entity, boolean includeSelf, ResourceLocation id, FriendlyByteBuf buffer) {
        PacketDistributor.PacketTarget target = includeSelf
                ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity)
                : PacketDistributor.TRACKING_ENTITY.with(() -> entity);
        target.send(NetworkManager.toPacket(NetworkManager.Side.S2C, id, buffer));
    }
}
