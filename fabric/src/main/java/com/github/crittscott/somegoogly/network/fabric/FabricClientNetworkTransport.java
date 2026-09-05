package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.concurrent.Executor;

/** Physical-client Fabric payload receivers and client-to-server sends. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installClientSender(ClientPlayNetworking::canSend, ClientPlayNetworking::send);
        NetworkHandler.registerClientReceivers(FabricClientNetworkTransport::registerReceiver);
    }

    private static <T> void registerReceiver(NetworkHandler.PayloadType<T> payloadType) {
        ClientPlayNetworking.registerGlobalReceiver(payloadType.type(),
                (payload, context) -> payloadType.receiveClientbound(payload, new FabricContext(context.client())));
    }

    private record FabricContext(Executor executor) implements NetworkTransport.Context {
        @Override
        public void queue(Runnable task) {
            executor.execute(task);
        }
    }
}
