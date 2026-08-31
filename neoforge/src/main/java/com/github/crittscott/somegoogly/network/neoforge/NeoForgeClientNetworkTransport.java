package com.github.crittscott.somegoogly.network.neoforge;

import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Physical-client NeoForge send capability and client-to-server transport. */
public final class NeoForgeClientNetworkTransport {

    private NeoForgeClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(type -> {
            var listener = Minecraft.getInstance().getConnection();
            return listener != null && NetworkRegistry.hasChannel(listener, type.id());
        }, PacketDistributor::sendToServer);
    }
}
