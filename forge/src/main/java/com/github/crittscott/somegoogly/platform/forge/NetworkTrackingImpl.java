package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.world.entity.Entity;

/** Forge entity-tracking distribution using its chunk-map-backed packet target. */
public final class NetworkTrackingImpl {

    private NetworkTrackingImpl() {
    }

    public static void send(Entity entity, boolean includeSelf, NetworkHandler.Payload<?> payload) {
        ForgeNetworkTransport.sendTracking(entity, includeSelf, payload);
    }
}
