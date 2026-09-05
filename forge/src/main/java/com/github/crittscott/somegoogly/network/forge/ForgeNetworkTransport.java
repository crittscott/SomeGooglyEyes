package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadConnection;

import java.util.Objects;

/** Required Forge 52 payload channel and native packet distribution. */
public final class ForgeNetworkTransport {

    private static Channel<CustomPacketPayload> channel;

    private ForgeNetworkTransport() {
    }

    public static void register() {
        ResourceLocation channelId = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "network");
        PayloadConnection<CustomPacketPayload> connection = ChannelBuilder
                .named(channelId)
                .networkProtocolVersion(Integer.parseInt(NetworkHandler.NETWORK_VERSION))
                .payloadChannel();

        connection.play().clientbound().addMain(
                EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        connection.play().clientbound().addMain(
                EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        connection.play().clientbound().addMain(
                EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        connection.play().serverbound().addMain(
                PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC,
                (payload, context) -> PickerFreezePacket.handle(payload, sender(context)));
        connection.play().serverbound().addMain(
                PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC,
                (payload, context) -> PickerExportPacket.handle(payload, sender(context)));

        channel = connection.play().bidirectional().build();
        NetworkTransport.installServerSender(
                (player, payload) -> channel.send(payload, PacketDistributor.PLAYER.with(player)));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }

    public static void sendTracking(Entity entity, boolean includeSelf, CustomPacketPayload payload) {
        channel.send(payload, includeSelf
                ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity)
                : PacketDistributor.TRACKING_ENTITY.with(entity));
    }

    private static ServerPlayer sender(CustomPayloadEvent.Context context) {
        return Objects.requireNonNull(context.getSender(), "Serverbound payload has no authenticated sender");
    }
}
