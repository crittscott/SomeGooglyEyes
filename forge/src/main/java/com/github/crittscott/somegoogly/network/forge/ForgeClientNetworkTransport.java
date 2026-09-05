package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.network.NetworkTransport;

/** Physical-client Forge client-to-server sender. */
public final class ForgeClientNetworkTransport {

    private ForgeClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(ForgeNetworkTransport::sendToServer);
    }
}
