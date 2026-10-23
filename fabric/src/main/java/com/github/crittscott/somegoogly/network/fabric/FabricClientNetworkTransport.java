package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Physical-client Fabric payload receivers. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    /** Register the client's receivers for every clientbound payload. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.PROTOCOL_HELLO,
                (client, handler, buffer, sender) -> ClientNetworkHandler.receiveHello(buffer));
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.EYE_STATE,
                (client, handler, buffer, sender) -> ClientNetworkHandler.receiveEyeState(buffer));
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.EYE_CONFIG,
                (client, handler, buffer, sender) -> ClientNetworkHandler.receiveEyeConfig(buffer));
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.EYE_BEHAVIOR,
                (client, handler, buffer, sender) -> ClientNetworkHandler.receiveBehavior(buffer));
    }
}
