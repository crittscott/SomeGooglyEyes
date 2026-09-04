package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadConnection;

/** Required Forge 52 payload channel, handlers, and packet distribution. */
public final class ForgeNetworkTransport {

    private static Channel<CustomPacketPayload> channel;

    private ForgeNetworkTransport() {
    }

    public static void register() {
        ResourceLocation channelId = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "network");
        PayloadConnection<CustomPacketPayload> connection = ChannelBuilder
                .named(channelId)
                .networkProtocolVersion(Integer.parseInt(NetworkHandler.PROTOCOL_VERSION))
                .payloadChannel();
        NetworkHandler.registerPayloads(new NetworkTransport.Registrar() {
            @Override
            public <T> void registerClientbound(NetworkHandler.PayloadType<T> payloadType) {
                connection.play().clientbound().add(payloadType.type(), payloadType.codec(),
                        (payload, context) -> receive(payloadType, payload, context));
            }

            @Override
            public <T> void registerServerbound(NetworkHandler.PayloadType<T> payloadType) {
                connection.play().serverbound().add(payloadType.type(), payloadType.codec(),
                        (payload, context) -> receive(payloadType, payload, context));
            }
        });
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

    public static boolean isRemotePresent(Connection connection) {
        return channel.isRemotePresent(connection);
    }

    private static <T> void receive(NetworkHandler.PayloadType<T> payloadType,
                                    NetworkHandler.Payload<T> payload,
                                    CustomPayloadEvent.Context context) {
        payloadType.receive(payload, new ForgeContext(context));
        context.setPacketHandled(true);
    }

    private record ForgeContext(CustomPayloadEvent.Context context) implements NetworkTransport.Context {
        @Override
        public ServerPlayer player() {
            return context.getSender();
        }

        @Override
        public void queue(Runnable task) {
            context.enqueueWork(task);
        }
    }
}
