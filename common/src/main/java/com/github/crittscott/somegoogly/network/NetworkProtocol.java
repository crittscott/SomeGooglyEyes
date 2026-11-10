package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's network version and the one declaration of every payload, its direction, and what handles it on
 * arrival; each loader registers from {@link #CLIENTBOUND} and {@link #SERVERBOUND} in list order. Clientbound
 * handlers reach {@link ClientNetworkHandler} only inside lambda bodies, so a dedicated server never links it.
 * Sends go straight through the loader bridges, {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking}.
 */
public final class NetworkProtocol {

    /** Every payload the server sends, with its client-thread handler. */
    public static final List<Clientbound<?>> CLIENTBOUND = List.of(
            new Clientbound<>(EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC,
                    packet -> ClientNetworkHandler.handleEyeState(packet)),
            new Clientbound<>(EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC,
                    packet -> ClientNetworkHandler.handleEyeConfigSync(packet)),
            new Clientbound<>(EyeSettingsPacket.TYPE, EyeSettingsPacket.STREAM_CODEC,
                    packet -> ClientNetworkHandler.handleEyeSettings(packet)),
            new Clientbound<>(EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC,
                    packet -> ClientNetworkHandler.handleBehavior(packet)),
            new Clientbound<>(EyeAngerPacket.TYPE, EyeAngerPacket.STREAM_CODEC,
                    packet -> ClientNetworkHandler.handleAnger(packet)));

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "18";

    /** Every payload a client sends, with its server-thread handler, which re-checks the sender's authority. */
    public static final List<Serverbound<?>> SERVERBOUND = List.of(
            new Serverbound<>(PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC, PickerFreezePacket::handle),
            new Serverbound<>(PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC, PickerExportPacket::handle));

    private NetworkProtocol() {
    }

    /** A payload the server sends. */
    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                             StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                             Consumer<T> handler) {
    }

    /** A payload a client sends. */
    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                             StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                             BiConsumer<T, ServerPlayer> handler) {
    }
}
