package com.github.crittscott.somegoogly.client.platform.fabric;

import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport.FabricPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric serverbound sends. */
public final class ClientNetworkingImpl {

    private ClientNetworkingImpl() {
    }

    public static void sendToServer(Payload payload) {
        ClientPlayNetworking.send(new FabricPayload(payload));
    }
}
