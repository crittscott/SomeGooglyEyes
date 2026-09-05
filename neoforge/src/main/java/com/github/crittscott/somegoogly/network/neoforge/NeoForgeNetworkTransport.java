package com.github.crittscott.somegoogly.network.neoforge;

import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge payload registration and server-side transport. */
public final class NeoForgeNetworkTransport {

    private NeoForgeNetworkTransport() {
    }

    public static void register(IEventBus modBus) {
        NetworkTransport.installServerSender(PacketDistributor::sendToPlayer);
        modBus.addListener(NeoForgeNetworkTransport::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("0").optional().executesOn(HandlerThread.NETWORK);
        NetworkHandler.registerPayloads(new NetworkTransport.Registrar() {
            @Override
            public <T> void registerClientbound(NetworkHandler.PayloadType<T> payloadType) {
                registrar.playToClient(payloadType.type(), payloadType.codec(),
                        (payload, context) -> payloadType.receiveClientbound(payload, new NeoForgeContext(context)));
            }

            @Override
            public <T> void registerServerbound(NetworkHandler.PayloadType<T> payloadType) {
                registrar.playToServer(payloadType.type(), payloadType.codec(),
                        (payload, context) -> payloadType.receiveServerbound(
                                payload, serverPlayer(context), new NeoForgeContext(context)));
            }
        });
    }

    private static ServerPlayer serverPlayer(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            return player;
        }
        throw new IllegalStateException("Serverbound payload has no authenticated server player");
    }

    private record NeoForgeContext(IPayloadContext context) implements NetworkTransport.Context {
        @Override
        public void queue(Runnable task) {
            context.enqueueWork(task);
        }
    }
}
