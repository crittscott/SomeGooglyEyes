package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.Executor;

/** Fabric play-payload codecs, server receivers, and direct server sends. */
public final class FabricNetworkTransport {

    private FabricNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installServerSender(ServerPlayNetworking::send);
        NetworkHandler.registerPayloads(new NetworkTransport.Registrar() {
            @Override
            public <T> void registerClientbound(NetworkHandler.PayloadType<T> payloadType) {
                PayloadTypeRegistry.playS2C().register(payloadType.type(), payloadType.codec());
            }

            @Override
            public <T> void registerServerbound(NetworkHandler.PayloadType<T> payloadType) {
                PayloadTypeRegistry.playC2S().register(payloadType.type(), payloadType.codec());
                ServerPlayNetworking.registerGlobalReceiver(payloadType.type(),
                        (payload, context) -> payloadType.receive(payload,
                                new FabricContext(context.player(), context.server())));
            }
        });
    }

    private record FabricContext(ServerPlayer player, Executor executor)
            implements NetworkTransport.Context {
        @Override
        public void queue(Runnable task) {
            executor.execute(task);
        }
    }
}
