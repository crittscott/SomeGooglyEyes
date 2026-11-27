package com.github.crittscott.somegoogly.client.platform;

import com.github.crittscott.somegoogly.network.Payload;

import dev.architectury.injectables.annotations.ExpectPlatform;

/** Physical-client loader bridge for native serverbound payload sends. */
public final class ClientNetworking {

    private ClientNetworking() {}

    // Architectury replaces this stub body with the loader implementation.
    @SuppressWarnings("DoNotCallSuggester")
    @ExpectPlatform
    public static void sendToServer(Payload payload) {
        throw new AssertionError();
    }
}
