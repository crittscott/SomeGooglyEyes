package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Physical-client loader bridge for native serverbound payload sends. */
public final class ClientNetworking {

    private ClientNetworking() {
    }

    /** Whether the connected server accepts payloads on {@code id}. */
    @ExpectPlatform
    public static boolean canSendToServer(ResourceLocation id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buffer) {
        throw new AssertionError();
    }
}
