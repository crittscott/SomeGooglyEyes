package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.Payload;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Fabric packet types and server networking, with play handlers dispatched by Fabric on the game thread. */
public final class FabricNetworkTransport {

    /**
     * Login-query channel the server asks about before the player joins. A client that answers it has the mod and
     * replies with its network version; one that does not understand it, or runs another version, is refused.
     */
    public static final ResourceLocation HANDSHAKE = SomeGooglyCommon.id("handshake");
    /** Longest network version a client's handshake reply may hold. */
    private static final int MAX_VERSION_LENGTH = 16;
    private static final Map<ResourceLocation, PacketType<FabricPayload>> PACKET_TYPES = packetTypes();

    private FabricNetworkTransport() {
    }

    /** Fabric's packet contract stays in this loader; the body and its decoder remain shared with Forge. */
    public record FabricPayload(Payload body) implements FabricPacket {
        @Override
        public PacketType<FabricPayload> getType() {
            return type(body.id());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            body.write(buffer);
        }
    }

    private static Map<ResourceLocation, PacketType<FabricPayload>> packetTypes() {
        Map<ResourceLocation, PacketType<FabricPayload>> types = new HashMap<>();
        for (Payload.Clientbound<?> payload : Payload.CLIENTBOUND) {
            types.put(payload.id(), PacketType.create(payload.id(),
                    buffer -> new FabricPayload(payload.reader().apply(buffer))));
        }
        for (Payload.Serverbound<?> payload : Payload.SERVERBOUND) {
            types.put(payload.id(), PacketType.create(payload.id(),
                    buffer -> new FabricPayload(payload.reader().apply(buffer))));
        }
        return Map.copyOf(types);
    }

    /** Register the login check and the serverbound receivers; called once from the mod initializer. */
    public static void register() {
        // Refuse a client without this network version before it joins the world.
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
                sender.sendPacket(HANDSHAKE, PacketByteBufs.empty()));
        ServerLoginNetworking.registerGlobalReceiver(HANDSHAKE,
                (server, handler, understood, buffer, synchronizer, responseSender) -> {
                    if (!understood) {
                        handler.disconnect(requiredClientMessage());
                        return;
                    }
                    String version = buffer.readUtf(MAX_VERSION_LENGTH);
                    if (!version.equals(NetworkProtocol.NETWORK_VERSION)) {
                        handler.disconnect(versionMismatchMessage(version));
                    }
                });
        for (Payload.Serverbound<?> payload : Payload.SERVERBOUND) {
            registerServerbound(payload);
        }
    }

    private static <T extends Payload> void registerServerbound(Payload.Serverbound<T> payload) {
        ServerPlayNetworking.registerGlobalReceiver(type(payload.id()), (packet, player, sender) ->
                payload.handler().accept(payload.type().cast(packet.body()), player));
    }

    /** A refusal that also reads right to a client lacking our language file; Fabric API loads it on the server. */
    private static Component refusal(String key, Object... arguments) {
        return Component.translatableWithFallback(key, Language.getInstance().getOrDefault(key), arguments);
    }

    /** The refusal shown to a client without this mod. */
    public static Component requiredClientMessage() {
        return refusal("somegoogly.network.required_client");
    }

    /** The single Fabric type for a shared payload id, used by registration and sends. */
    public static PacketType<FabricPayload> type(ResourceLocation id) {
        return PACKET_TYPES.get(id);
    }

    /** The refusal shown to a client running network version {@code clientVersion}. */
    public static Component versionMismatchMessage(String clientVersion) {
        return refusal("somegoogly.network.version_mismatch", NetworkProtocol.NETWORK_VERSION, clientVersion);
    }
}
