package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkContext;

/** Physical-client Forge capability check and client-to-server sender. */
public final class ForgeClientNetworkTransport {

    private ForgeClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(type -> {
            var listener = Minecraft.getInstance().getConnection();
            return listener != null && NetworkContext.get(listener.getConnection())
                    .getRemoteChannels().contains(type.id());
        }, ForgeNetworkTransport::sendToServer);
    }
}
