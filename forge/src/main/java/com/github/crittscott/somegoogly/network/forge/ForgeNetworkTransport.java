package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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

    /** Build the channel and register every payload; called once from the mod constructor, before any send. */
    public static void register() {
        ResourceLocation channelId = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "network");
        PayloadConnection<CustomPacketPayload> connection = ChannelBuilder
                .named(channelId)
                .networkProtocolVersion(Integer.parseInt(NetworkProtocol.NETWORK_VERSION))
                .payloadChannel();

        connection.play().clientbound().addMain(
                EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeState(payload));
        connection.play().clientbound().addMain(
                EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload));
        connection.play().clientbound().addMain(
                EyeSettingsPacket.TYPE, EyeSettingsPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeSettings(payload));
        connection.play().clientbound().addMain(
                EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleBehavior(payload));
        connection.play().clientbound().addMain(
                EyeAngerPacket.TYPE, EyeAngerPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleAnger(payload));
        connection.play().serverbound().addMain(
                PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC,
                (payload, context) -> PickerFreezePacket.handle(payload, context.getSender()));
        connection.play().serverbound().addMain(
                PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC,
                (payload, context) -> PickerExportPacket.handle(payload, context.getSender()));

        channel = connection.play().bidirectional().build();
    }
}
