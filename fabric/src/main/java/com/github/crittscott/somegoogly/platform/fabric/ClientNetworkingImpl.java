package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.network.Payload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric serverbound sends. */
public final class ClientNetworkingImpl {

    private ClientNetworkingImpl() {
    }

    public static void sendToServer(Payload payload) {
        ClientPlayNetworking.send(payload.id(), NetworkingImpl.encode(payload));
    }
}
