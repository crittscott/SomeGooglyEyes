package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.world.entity.Entity;

/** Loader bridge for Minecraft's exact entity-tracking recipient set. */
public final class NetworkTracking {

    private NetworkTracking() {
    }

    /**
     * Send a clientbound payload to every player tracking {@code entity}. When {@code includeSelf} is
     * true and the entity is a server player, that player is also a recipient.
     */
    @ExpectPlatform
    public static void send(Entity entity, boolean includeSelf, NetworkHandler.Payload<?> payload) {
        throw new AssertionError();
    }
}
