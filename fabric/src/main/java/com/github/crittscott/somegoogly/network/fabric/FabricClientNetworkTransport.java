package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Physical-client Fabric payload receivers. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    /** Register the client's version answer, its refusal of a server without the mod, and its payload receivers. */
    public static void register() {
        ClientConfigurationNetworking.registerGlobalReceiver(FabricNetworkTransport.Handshake.TYPE,
                (payload, context) -> context.responseSender().sendPacket(
                        new FabricNetworkTransport.HandshakeReply(NetworkProtocol.NETWORK_VERSION)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ClientPlayNetworking.canSend(PickerFreezePacket.TYPE)) {
                handler.getConnection().disconnect(Component.translatable("somegoogly.network.required_server"));
            }
        });
        for (NetworkProtocol.Clientbound<?> payload : NetworkProtocol.CLIENTBOUND) {
            registerReceiver(payload);
        }
    }

    private static <T extends CustomPacketPayload> void registerReceiver(NetworkProtocol.Clientbound<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (packet, context) -> payload.handler().accept(packet));
    }
}
