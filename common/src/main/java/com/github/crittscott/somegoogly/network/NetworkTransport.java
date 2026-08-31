package com.github.crittscott.somegoogly.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Loader-neutral installation point for native play-payload registration and sends. */
public final class NetworkTransport {

    @FunctionalInterface
    public interface Receiver<T> {
        void receive(T value, Context context);
    }

    public interface Context {
        @Nullable
        ServerPlayer player();

        void queue(Runnable task);
    }

    public interface Registrar {
        <T> void registerClientbound(NetworkHandler.PayloadType<T> payloadType);

        <T> void registerServerbound(NetworkHandler.PayloadType<T> payloadType);
    }

    @FunctionalInterface
    public interface ClientReceiverRegistrar {
        <T> void register(NetworkHandler.PayloadType<T> payloadType);
    }

    private static BiConsumer<ServerPlayer, CustomPacketPayload> serverSender;
    private static Predicate<CustomPacketPayload.Type<?>> serverCapability;
    private static Consumer<CustomPacketPayload> clientSender;

    private NetworkTransport() {
    }

    public static synchronized void installServerSender(
            BiConsumer<ServerPlayer, CustomPacketPayload> sender) {
        if (serverSender != null) {
            throw new IllegalStateException("Server network sender is already installed");
        }
        serverSender = Objects.requireNonNull(sender);
    }

    public static synchronized void installClientSender(
            Predicate<CustomPacketPayload.Type<?>> capability,
            Consumer<CustomPacketPayload> sender) {
        if (serverCapability != null || clientSender != null) {
            throw new IllegalStateException("Client network sender is already installed");
        }
        serverCapability = Objects.requireNonNull(capability);
        clientSender = Objects.requireNonNull(sender);
    }

    static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (serverSender == null) {
            throw new IllegalStateException("Server network sender is not installed");
        }
        serverSender.accept(player, payload);
    }

    static boolean canServerReceive(CustomPacketPayload.Type<?> type) {
        return serverCapability != null && serverCapability.test(type);
    }

    static void sendToServer(CustomPacketPayload payload) {
        if (clientSender == null) {
            throw new IllegalStateException("Client network sender is not installed");
        }
        clientSender.accept(payload);
    }
}
