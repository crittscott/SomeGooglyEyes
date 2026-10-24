package com.github.crittscott.somegoogly.platform;

import com.github.crittscott.somegoogly.network.Payload;
import dev.architectury.injectables.annotations.ExpectPlatform;

/** Physical-client loader bridge for native serverbound payload sends. */
public final class ClientNetworking {

    private ClientNetworking() {
    }

    @ExpectPlatform
    public static void sendToServer(Payload payload) {
        throw new AssertionError();
    }
}
