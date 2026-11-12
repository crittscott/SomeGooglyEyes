package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadConnection;

/**
 * Required Forge payload channel. The channel's version is the network version, and both sides require it, so Forge
 * refuses a client or server without a matching mod.
 */
public final class ForgeNetworkTransport {

    /** The payload channel, built once by {@link #register}, before any send. */
    public static Channel<CustomPacketPayload> channel;

    private ForgeNetworkTransport() {
    }

    private static <T extends CustomPacketPayload> void addClientbound(
            PayloadConnection<CustomPacketPayload> connection, NetworkProtocol.Clientbound<T> payload) {
        connection.play().clientbound().addMain(payload.type(), payload.codec(),
                (packet, context) -> payload.handler().accept(packet));
    }

    private static <T extends CustomPacketPayload> void addServerbound(
            PayloadConnection<CustomPacketPayload> connection, NetworkProtocol.Serverbound<T> payload) {
        connection.play().serverbound().addMain(payload.type(), payload.codec(),
                (packet, context) -> payload.handler().accept(packet, context.getSender()));
    }

    /** Build the channel and register every payload; called once from the mod constructor, before any send. */
    public static void register() {
        PayloadConnection<CustomPacketPayload> connection = ChannelBuilder
                .named(SomeGooglyCommon.id("network"))
                .networkProtocolVersion(Integer.parseInt(NetworkProtocol.NETWORK_VERSION))
                .payloadChannel();

        for (NetworkProtocol.Clientbound<?> payload : NetworkProtocol.CLIENTBOUND) {
            addClientbound(connection, payload);
        }
        for (NetworkProtocol.Serverbound<?> payload : NetworkProtocol.SERVERBOUND) {
            addServerbound(connection, payload);
        }

        channel = connection.play().bidirectional().build();
    }
}
