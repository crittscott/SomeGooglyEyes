package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.function.Consumer;

/** Fabric payload codecs, the configuration-phase version check, and server receivers. */
public final class FabricNetworkTransport {

    /** Longest network version a client's handshake reply may hold. */
    private static final int MAX_VERSION_LENGTH = 16;

    /**
     * The server's version question, the first exchange of a connection's configuration phase. A client that
     * lacks a receiver for it is refused; one that has it answers with a {@link HandshakeReply}.
     */
    public record Handshake() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Handshake> TYPE =
                new CustomPacketPayload.Type<>(SomeGooglyCommon.id("handshake"));
        public static final StreamCodec<FriendlyByteBuf, Handshake> STREAM_CODEC = StreamCodec.unit(new Handshake());

        @Override
        public CustomPacketPayload.Type<Handshake> type() {
            return TYPE;
        }
    }

    /** The client's network version, which must equal the server's. */
    public record HandshakeReply(String version) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakeReply> TYPE =
                new CustomPacketPayload.Type<>(SomeGooglyCommon.id("handshake_reply"));
        public static final StreamCodec<ByteBuf, HandshakeReply> STREAM_CODEC = ByteBufCodecs.stringUtf8(MAX_VERSION_LENGTH)
                .map(HandshakeReply::new, HandshakeReply::version);

        @Override
        public CustomPacketPayload.Type<HandshakeReply> type() {
            return TYPE;
        }
    }

    /** Holds the player in the configuration phase until the client's {@link HandshakeReply} arrives. */
    private record HandshakeTask() implements ConfigurationTask {
        static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(SomeGooglyCommon.id("handshake").toString());

        @Override
        public void start(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createClientboundPacket(new Handshake()));
        }

        @Override
        public ConfigurationTask.Type type() {
            return TYPE;
        }
    }

    private FabricNetworkTransport() {
    }

    /** Register every payload type, the version check, and the serverbound receivers; called once from the mod initializer. */
    public static void register() {
        PayloadTypeRegistry.clientboundConfiguration().register(Handshake.TYPE, Handshake.STREAM_CODEC);
        PayloadTypeRegistry.serverboundConfiguration().register(HandshakeReply.TYPE, HandshakeReply.STREAM_CODEC);
        for (NetworkProtocol.Clientbound<?> payload : NetworkProtocol.CLIENTBOUND) {
            registerType(payload);
        }
        for (NetworkProtocol.Serverbound<?> payload : NetworkProtocol.SERVERBOUND) {
            registerType(payload);
            registerReceiver(payload);
        }

        // Refuse a client without this network version before it joins the world.
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, Handshake.TYPE)) {
                handler.addTask(new HandshakeTask());
            } else {
                handler.disconnect(requiredClientMessage());
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(HandshakeReply.TYPE,
                (payload, context) -> checkVersion(context.packetListener(), payload.version()));
    }

    private static void checkVersion(ServerConfigurationPacketListenerImpl handler, String version) {
        if (version.equals(NetworkProtocol.NETWORK_VERSION)) {
            handler.completeTask(HandshakeTask.TYPE);
        } else {
            handler.disconnect(versionMismatchMessage(version));
        }
    }

    private static <T extends CustomPacketPayload> void registerReceiver(NetworkProtocol.Serverbound<T> payload) {
        ServerPlayNetworking.registerGlobalReceiver(payload.type(),
                (packet, context) -> payload.handler().accept(packet, context.player()));
    }

    private static <T extends CustomPacketPayload> void registerType(NetworkProtocol.Clientbound<T> payload) {
        PayloadTypeRegistry.clientboundPlay().register(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerType(NetworkProtocol.Serverbound<T> payload) {
        PayloadTypeRegistry.serverboundPlay().register(payload.type(), payload.codec());
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
