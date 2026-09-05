package com.github.crittscott.somegoogly.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Loader-installed native send operations and the physical-client receive handoff. */
public final class NetworkTransport {

    private static BiConsumer<ServerPlayer, CustomPacketPayload> serverSender;
    private static Consumer<CustomPacketPayload> clientSender;
    private static Consumer<CustomPacketPayload> clientReceiver;

    private NetworkTransport() {
    }

    public static synchronized void installServerSender(
            BiConsumer<ServerPlayer, CustomPacketPayload> sender) {
        if (serverSender != null) {
            throw new IllegalStateException("Server network sender is already installed");
        }
        serverSender = Objects.requireNonNull(sender);
    }

    public static synchronized void installClientSender(Consumer<CustomPacketPayload> sender) {
        if (clientSender != null) {
            throw new IllegalStateException("Client network sender is already installed");
        }
        clientSender = Objects.requireNonNull(sender);
    }

    public static synchronized void installClientReceiver(Consumer<CustomPacketPayload> receiver) {
        if (clientReceiver != null) {
            throw new IllegalStateException("Client network receiver is already installed");
        }
        clientReceiver = Objects.requireNonNull(receiver);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (serverSender == null) {
            throw new IllegalStateException("Server network sender is not installed");
        }
        serverSender.accept(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        if (clientSender == null) {
            throw new IllegalStateException("Client network sender is not installed");
        }
        clientSender.accept(payload);
    }

    public static void receiveClientbound(CustomPacketPayload payload) {
        if (clientReceiver == null) {
            throw new IllegalStateException("Client network receiver is not installed");
        }
        clientReceiver.accept(payload);
    }
}
