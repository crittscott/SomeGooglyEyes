package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.minecraft.client.Minecraft;

/** Physical-client Forge capability check and client-to-server sender. */
public final class ForgeClientNetworkTransport {

    private ForgeClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(type -> {
            var listener = Minecraft.getInstance().getConnection();
            return listener != null && ForgeNetworkTransport.isRemotePresent(listener.getConnection());
        }, ForgeNetworkTransport::sendToServer);
    }
}
