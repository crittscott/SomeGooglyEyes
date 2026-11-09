package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

/** Physical-client Fabric payload receivers; each decodes on the network thread and handles on the client thread. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    /** Register the client's login-query answer, its refusal of a server without the mod, and its payload receivers. */
    public static void register() {
        ClientLoginNetworking.registerGlobalReceiver(FabricNetworkTransport.HANDSHAKE,
                (client, handler, buffer, listenerAdder) -> CompletableFuture.completedFuture(
                        PacketByteBufs.create().writeUtf(NetworkProtocol.NETWORK_VERSION)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ClientPlayNetworking.canSend(PickerFreezePacket.ID)) {
                handler.getConnection().disconnect(Component.translatable("somegoogly.network.required_server"));
            }
        });
        for (Payload.Clientbound<?> payload : Payload.CLIENTBOUND) {
            registerClientbound(payload);
        }
    }

    private static <T extends Payload> void registerClientbound(Payload.Clientbound<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.id(), (client, handler, buffer, sender) -> {
            T packet = payload.reader().apply(buffer);
            client.execute(() -> payload.handler().accept(packet));
        });
    }
}
