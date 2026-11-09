package com.github.crittscott.somegoogly.client.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.chat.Component;

/** Physical-client Fabric login checks and typed play receivers dispatched on the client game thread. */
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
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworkTransport.type(payload.id()), (packet, player, sender) ->
                payload.handler().accept(payload.type().cast(packet.body())));
    }
}
