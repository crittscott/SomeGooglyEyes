package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/** Fabric server receivers; each decodes on the network thread and handles on the server thread. */
public final class FabricNetworkTransport {

    /**
     * Login-query channel the server asks about before the player joins. A client that answers it declares
     * that it runs this network version; one that does not understand it is refused.
     */
    public static final ResourceLocation HANDSHAKE = NetworkHandler.versioned("handshake");

    private FabricNetworkTransport() {
    }

    /** Register the serverbound receivers; called once from the mod initializer. */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PickerFreezePacket.ID, (server, player, handler, buffer, sender) -> {
            PickerFreezePacket packet = PickerFreezePacket.read(buffer);
            server.execute(() -> PickerFreezePacket.handle(packet, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(PickerExportPacket.ID, (server, player, handler, buffer, sender) -> {
            PickerExportPacket packet = PickerExportPacket.read(buffer);
            server.execute(() -> PickerExportPacket.handle(packet, player));
        });
    }
}
