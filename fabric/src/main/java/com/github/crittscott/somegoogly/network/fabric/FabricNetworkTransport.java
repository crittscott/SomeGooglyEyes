package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
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
     * Login-query channel the server asks about before the player joins. A client that answers it has the mod and
     * replies with its network version; one that does not understand it, or runs another version, is refused.
     */
    public static final ResourceLocation HANDSHAKE = NetworkProtocol.id("handshake");
    /** Longest network version a client's handshake reply may hold. */
    private static final int MAX_VERSION_LENGTH = 16;

    private FabricNetworkTransport() {
    }

    /** Register the login check and the serverbound receivers; called once from the mod initializer. */
    public static void register() {
        // Refuse a client without this network version before it joins the world.
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
                sender.sendPacket(HANDSHAKE, PacketByteBufs.empty()));
        ServerLoginNetworking.registerGlobalReceiver(HANDSHAKE,
                (server, handler, understood, buffer, synchronizer, responseSender) -> {
                    if (!understood) {
                        SomeGooglyCommon.LOGGER.info("Refusing {}: client lacks {}",
                                handler.getUserName(), SomeGooglyCommon.MOD_NAME);
                        handler.disconnect(requiredClientMessage());
                        return;
                    }
                    String version = buffer.readUtf(MAX_VERSION_LENGTH);
                    if (!version.equals(NetworkProtocol.NETWORK_VERSION)) {
                        SomeGooglyCommon.LOGGER.info("Refusing {}: client runs {} network version {}, server runs {}",
                                handler.getUserName(), SomeGooglyCommon.MOD_NAME, version, NetworkProtocol.NETWORK_VERSION);
                        handler.disconnect(versionMismatchMessage(version));
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

    /** A refusal that also reads right to a client lacking our language file; Fabric API loads it on the server. */
    private static Component refusal(String key, Object... arguments) {
        return Component.translatableWithFallback(key, Language.getInstance().getOrDefault(key), arguments);
    }

    /** The refusal shown to a client without this mod. */
    public static Component requiredClientMessage() {
        return refusal("somegoogly.network.required_client");
    }

    /** The refusal shown to a client running network version {@code clientVersion}. */
    public static Component versionMismatchMessage(String clientVersion) {
        return refusal("somegoogly.network.version_mismatch", NetworkProtocol.NETWORK_VERSION, clientVersion);
    }
}
