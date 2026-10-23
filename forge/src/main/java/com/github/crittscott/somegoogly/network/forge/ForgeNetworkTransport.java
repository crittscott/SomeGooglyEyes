package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.github.crittscott.somegoogly.network.PickerMobPosePacket;
import com.github.crittscott.somegoogly.network.PickerSpawnAllPacket;
import com.github.crittscott.somegoogly.network.PickerSpawnPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Required Forge payload channel and native packet distribution. The common protocol addresses payloads by
 * id, so each travels on this one versioned channel as an id-tagged message and is dispatched by that id.
 */
public final class ForgeNetworkTransport {

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SomeGooglyCommon.MOD_ID, "network"),
            () -> NetworkHandler.PROTOCOL_VERSION,
            NetworkHandler.PROTOCOL_VERSION::equals,
            NetworkHandler.PROTOCOL_VERSION::equals);

    private static Map<ResourceLocation, BiConsumer<FriendlyByteBuf, ServerPlayer>> serverbound;
    private static Map<ResourceLocation, Consumer<FriendlyByteBuf>> clientbound;

    /** One payload: its common id and its encoded body. */
    private record Payload(ResourceLocation id, FriendlyByteBuf body) {

        void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(id);
            buffer.writeBytes(body, body.readerIndex(), body.readableBytes());
        }

        static Payload decode(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            byte[] body = new byte[buffer.readableBytes()];
            buffer.readBytes(body);
            return new Payload(id, new FriendlyByteBuf(Unpooled.wrappedBuffer(body)));
        }
    }

    private ForgeNetworkTransport() {
    }

    /** Register the channel message and every receiver; called once from the mod constructor, before any send. */
    public static void register() {
        serverbound = Map.of(
                NetworkHandler.PROTOCOL_ACK, NetworkHandler::receiveAcknowledgement,
                NetworkHandler.PICKER_FREEZE,
                (body, player) -> PickerFreezePacket.handle(PickerFreezePacket.decode(body), player),
                NetworkHandler.PICKER_SPAWN,
                (body, player) -> PickerSpawnPacket.handle(PickerSpawnPacket.decode(body), player),
                NetworkHandler.PICKER_SPAWN_ALL,
                (body, player) -> PickerSpawnAllPacket.handle(PickerSpawnAllPacket.decode(body), player),
                NetworkHandler.PICKER_MOB_POSE,
                (body, player) -> PickerMobPosePacket.handle(PickerMobPosePacket.decode(body), player),
                NetworkHandler.PICKER_EXPORT,
                (body, player) -> PickerExportPacket.handle(PickerExportPacket.decode(body), player));
        // Lambda bodies, not method references, so a dedicated server never links the client handler.
        clientbound = Map.of(
                NetworkHandler.PROTOCOL_HELLO, body -> ClientNetworkHandler.receiveHello(body),
                NetworkHandler.EYE_STATE, body -> ClientNetworkHandler.receiveEyeState(body),
                NetworkHandler.EYE_CONFIG, body -> ClientNetworkHandler.receiveEyeConfig(body),
                NetworkHandler.EYE_BEHAVIOR, body -> ClientNetworkHandler.receiveBehavior(body));

        CHANNEL.messageBuilder(Payload.class, 0)
                .encoder(Payload::encode)
                .decoder(Payload::decode)
                .consumerNetworkThread(ForgeNetworkTransport::receive)
                .add();
    }

    /** Send to the players {@code target} selects. */
    public static void send(PacketDistributor.PacketTarget target, ResourceLocation id, FriendlyByteBuf body) {
        CHANNEL.send(target, new Payload(id, body));
    }

    /** Send from the client to the server. */
    public static void sendToServer(ResourceLocation id, FriendlyByteBuf body) {
        CHANNEL.sendToServer(new Payload(id, body));
    }

    /** Whether the other end of {@code connection} has this channel. */
    public static boolean isRemotePresent(Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    }

    private static void receive(Payload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            BiConsumer<FriendlyByteBuf, ServerPlayer> handler = serverbound.get(payload.id());
            ServerPlayer sender = context.getSender();
            if (handler != null && sender != null) {
                handler.accept(payload.body(), sender);
            }
        } else {
            Consumer<FriendlyByteBuf> handler = clientbound.get(payload.id());
            if (handler != null) {
                handler.accept(payload.body());
            }
        }
        context.setPacketHandled(true);
    }
}
