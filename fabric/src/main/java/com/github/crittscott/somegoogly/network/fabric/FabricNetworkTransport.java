package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
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
                new CustomPacketPayload.Type<>(NetworkProtocol.id("handshake"));
        public static final StreamCodec<FriendlyByteBuf, Handshake> STREAM_CODEC = StreamCodec.unit(new Handshake());

        @Override
        public CustomPacketPayload.Type<Handshake> type() {
            return TYPE;
        }
    }

    /** The client's network version, which must equal the server's. */
    public record HandshakeReply(String version) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakeReply> TYPE =
                new CustomPacketPayload.Type<>(NetworkProtocol.id("handshake_reply"));
        public static final StreamCodec<ByteBuf, HandshakeReply> STREAM_CODEC = ByteBufCodecs.stringUtf8(MAX_VERSION_LENGTH)
                .map(HandshakeReply::new, HandshakeReply::version);

        @Override
        public CustomPacketPayload.Type<HandshakeReply> type() {
            return TYPE;
        }
    }

    /** Holds the player in the configuration phase until the client's {@link HandshakeReply} arrives. */
    private record HandshakeTask() implements ConfigurationTask {
        static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(NetworkProtocol.id("handshake").toString());

        @Override
        public void start(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new Handshake()));
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
        PayloadTypeRegistry.configurationS2C().register(Handshake.TYPE, Handshake.STREAM_CODEC);
        PayloadTypeRegistry.configurationC2S().register(HandshakeReply.TYPE, HandshakeReply.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeSettingsPacket.TYPE, EyeSettingsPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeAngerPacket.TYPE, EyeAngerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC);

        // Refuse a client without this network version before it joins the world.
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, Handshake.TYPE)) {
                handler.addTask(new HandshakeTask());
            } else {
                SomeGooglyCommon.LOGGER.info("Refusing {}: client lacks {}",
                        handler.getOwner().getName(), SomeGooglyCommon.MOD_NAME);
                handler.disconnect(requiredClientMessage());
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(HandshakeReply.TYPE,
                (payload, context) -> checkVersion(context.networkHandler(), payload.version()));

        ServerPlayNetworking.registerGlobalReceiver(PickerFreezePacket.TYPE,
                (payload, context) -> PickerFreezePacket.handle(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(PickerExportPacket.TYPE,
                (payload, context) -> PickerExportPacket.handle(payload, context.player()));
    }

    private static void checkVersion(ServerConfigurationPacketListenerImpl handler, String version) {
        if (version.equals(NetworkProtocol.NETWORK_VERSION)) {
            handler.completeTask(HandshakeTask.TYPE);
        } else {
            SomeGooglyCommon.LOGGER.info("Refusing {}: client runs {} network version {}, server runs {}",
                    handler.getOwner().getName(), SomeGooglyCommon.MOD_NAME, version, NetworkProtocol.NETWORK_VERSION);
            handler.disconnect(versionMismatchMessage(version));
        }
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
