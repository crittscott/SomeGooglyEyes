package com.github.crittscott.somegoogly.network.neoforge;

import com.github.crittscott.somegoogly.network.NetworkProtocol;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Required NeoForge payload registration and server receivers. */
public final class NeoForgeNetworkTransport {

    private NeoForgeNetworkTransport() {
    }

    /** Register the payloads when the mod bus fires its payload event; called once from the mod constructor. */
    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeNetworkTransport::registerPayloads);
    }

    private static <T extends CustomPacketPayload> void registerClientbound(
            PayloadRegistrar registrar, NetworkProtocol.Clientbound<T> payload) {
        registrar.playToClient(payload.type(), payload.codec(), (packet, context) -> payload.handler().accept(packet));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NetworkProtocol.NETWORK_VERSION);
        for (NetworkProtocol.Clientbound<?> payload : NetworkProtocol.CLIENTBOUND) {
            registerClientbound(registrar, payload);
        }
        for (NetworkProtocol.Serverbound<?> payload : NetworkProtocol.SERVERBOUND) {
            registerServerbound(registrar, payload);
        }
    }

    private static <T extends CustomPacketPayload> void registerServerbound(
            PayloadRegistrar registrar, NetworkProtocol.Serverbound<T> payload) {
        registrar.playToServer(payload.type(), payload.codec(),
                (packet, context) -> payload.handler().accept(packet, (ServerPlayer) context.player()));
    }
}
