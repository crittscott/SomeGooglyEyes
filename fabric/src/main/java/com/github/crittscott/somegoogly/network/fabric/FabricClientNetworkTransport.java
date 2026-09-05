package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Physical-client Fabric payload receivers and client-to-server sends. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(EyeStatePacket.TYPE,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeConfigSyncPacket.TYPE,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeBehaviorTriggerPacket.TYPE,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
    }
}
