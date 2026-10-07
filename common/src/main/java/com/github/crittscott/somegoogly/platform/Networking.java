package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Loader bridge for native clientbound payload sends. */
public final class Networking {

    private Networking() {
    }

    /** Send a clientbound payload to one player. */
    @ExpectPlatform
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        throw new AssertionError();
    }

    /**
     * Send a clientbound payload to every player tracking {@code entity} and, when the entity is itself
     * a server player, to that player too.
     */
    @ExpectPlatform
    public static void sendTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        throw new AssertionError();
    }
}
