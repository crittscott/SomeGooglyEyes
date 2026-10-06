package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Physical-client loader bridge for native serverbound payload sends. */
public final class ClientNetworking {

    private ClientNetworking() {
    }

    @ExpectPlatform
    public static void sendToServer(CustomPacketPayload payload) {
        throw new AssertionError();
    }
}
