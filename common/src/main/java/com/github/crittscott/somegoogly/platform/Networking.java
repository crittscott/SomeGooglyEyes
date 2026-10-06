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
     * Send a clientbound payload to every player tracking {@code entity}. When {@code includeSelf} is
     * true and the entity is a server player, that player is also a recipient.
     */
    @ExpectPlatform
    public static void sendTracking(Entity entity, boolean includeSelf, CustomPacketPayload payload) {
        throw new AssertionError();
    }
}
