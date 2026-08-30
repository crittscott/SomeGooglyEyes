package com.github.crittscott.somegoogly.platform.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge entity-tracking distribution through the authoritative chunk-map fanout. */
public final class NetworkTrackingImpl {

    private NetworkTrackingImpl() {
    }

    public static void send(Entity entity, boolean includeSelf, CustomPacketPayload payload) {
        if (includeSelf) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        }
    }
}
