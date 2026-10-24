package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;

/** Forge serverbound sends through the mod's payload channel. */
public final class ClientNetworkingImpl {

    private ClientNetworkingImpl() {
    }

    public static void sendToServer(Payload payload) {
        ForgeNetworkTransport.sendToServer(payload);
    }
}
