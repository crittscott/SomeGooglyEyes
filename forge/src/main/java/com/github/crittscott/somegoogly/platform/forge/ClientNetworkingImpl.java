package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.network.forge.ForgeNetworkTransport;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Forge serverbound sends through the mod's payload channel. */
public final class ClientNetworkingImpl {

    private ClientNetworkingImpl() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ForgeNetworkTransport.sendToServer(payload);
    }
}
