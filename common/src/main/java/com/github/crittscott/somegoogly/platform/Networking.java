package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Loader bridge for native clientbound payload sends. */
public final class Networking {

    private Networking() {
    }

    /** Send a clientbound payload to one player. */
    @ExpectPlatform
    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buffer) {
        throw new AssertionError();
    }

    /**
     * Send a clientbound payload to every player tracking {@code entity} and, when the entity is itself
     * a server player, to that player too.
     */
    @ExpectPlatform
    public static void sendTrackingAndSelf(Entity entity, ResourceLocation id, FriendlyByteBuf buffer) {
        throw new AssertionError();
    }
}
