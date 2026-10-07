package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
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

/** Required Forge payload channel and native packet distribution. */
public final class ForgeNetworkTransport {

    private static Channel<CustomPacketPayload> channel;

    private ForgeNetworkTransport() {
    }

    /** Build the channel and register every payload; called once from the mod constructor, before any send. */
    public static void register() {
        ResourceLocation channelId = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "network");
        PayloadConnection<CustomPacketPayload> connection = ChannelBuilder
                .named(channelId)
                .networkProtocolVersion(Integer.parseInt(NetworkHandler.NETWORK_VERSION))
                .payloadChannel();

        connection.play().clientbound().addMain(
                EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeState(payload));
        connection.play().clientbound().addMain(
                EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload));
        connection.play().clientbound().addMain(
                EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleBehavior(payload));
        connection.play().serverbound().addMain(
                PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC,
                (payload, context) -> PickerFreezePacket.handle(payload, sender(context)));
        connection.play().serverbound().addMain(
                PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC,
                (payload, context) -> PickerExportPacket.handle(payload, sender(context)));

        channel = connection.play().bidirectional().build();
    }

    /** Send to one player. */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.PLAYER.with(player));
    }

    /** Send from the client to the server. */
    public static void sendToServer(CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }

    /** Send to every player tracking {@code entity}, and to the entity itself if it is a player. */
    public static void sendTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity));
    }

    private static ServerPlayer sender(CustomPayloadEvent.Context context) {
        return Objects.requireNonNull(context.getSender(), "Serverbound payload has no authenticated sender");
    }
}
