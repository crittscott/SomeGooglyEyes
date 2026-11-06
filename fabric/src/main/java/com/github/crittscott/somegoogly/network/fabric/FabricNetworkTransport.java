package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Fabric server networking: the login version check, and receivers that decode on the network thread and handle on the server thread. */
public final class FabricNetworkTransport {

    /**
     * Login-query channel the server asks about before the player joins. A client that answers it declares
     * that it runs this network version; one that does not understand it is refused.
     */
    public static final ResourceLocation HANDSHAKE = NetworkHandler.versioned("handshake");

    private FabricNetworkTransport() {
    }

    /** Register the login check and the serverbound receivers; called once from the mod initializer. */
    public static void register() {
        // Refuse a client without this network version before it joins the world: it cannot answer the query.
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
                sender.sendPacket(HANDSHAKE, PacketByteBufs.empty()));
        ServerLoginNetworking.registerGlobalReceiver(HANDSHAKE,
                (server, handler, understood, buffer, synchronizer, responseSender) -> {
                    if (!understood) {
                        SomeGooglyCommon.LOGGER.info("Refusing {}: client lacks {} network version {}",
                                handler.getUserName(), SomeGooglyCommon.MOD_NAME, NetworkHandler.NETWORK_VERSION);
                        handler.disconnect(requiredClientMessage());
                    }
                });
        ServerPlayNetworking.registerGlobalReceiver(PickerFreezePacket.ID, (server, player, handler, buffer, sender) -> {
            PickerFreezePacket packet = PickerFreezePacket.read(buffer);
            server.execute(() -> PickerFreezePacket.handle(packet, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(PickerExportPacket.ID, (server, player, handler, buffer, sender) -> {
            PickerExportPacket packet = PickerExportPacket.read(buffer);
            server.execute(() -> PickerExportPacket.handle(packet, player));
        });
    }

    /**
     * The refusal shown to a client without this mod. That client lacks our language file, so the text travels
     * with the key; Fabric API loads the mod's language file on the server.
     */
    public static Component requiredClientMessage() {
        String key = "somegoogly.network.required_client";
        return Component.translatableWithFallback(key, Language.getInstance().getOrDefault(key));
    }
}
