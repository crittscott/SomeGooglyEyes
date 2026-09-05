package com.github.crittscott.somegoogly.network.neoforge;

import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.neoforged.neoforge.network.PacketDistributor;

/** Physical-client NeoForge client-to-server transport. */
public final class NeoForgeClientNetworkTransport {

    private NeoForgeClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(PacketDistributor::sendToServer);
    }
}
