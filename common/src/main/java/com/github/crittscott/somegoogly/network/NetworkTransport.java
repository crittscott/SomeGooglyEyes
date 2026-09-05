package com.github.crittscott.somegoogly.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Loader-neutral installation point for native play-payload registration and sends. Loader adapters
 * install their physical-side senders once during bootstrap and translate native receive contexts into
 * {@link Context}; packet handlers remain loader-neutral.
 */
public final class NetworkTransport {

    /** Receives a decoded clientbound payload, potentially on a loader networking thread. */
    @FunctionalInterface
    public interface ClientReceiver<T> {
        /** Handle {@code value}; queue game-state work through {@link Context#queue}. */
        void receive(T value, Context context);
    }

    /** Receives a decoded serverbound payload together with its authenticated sender. */
    @FunctionalInterface
    public interface ServerReceiver<T> {
        /** Handle {@code value}; queue game-state work through {@link Context#queue}. */
        void receive(T value, ServerPlayer player, Context context);
    }

    /** The main-thread handoff supplied with a received payload. */
    public interface Context {
        /** Schedule {@code task} on the receiving logical side's main game thread. */
        void queue(Runnable task);
    }

    /** Direction-specific codec and server-receiver registration used during loader network setup. */
    public interface Registrar {
        /** Register one clientbound payload's codec and, where required by the loader, its receiver. */
        <T> void registerClientbound(NetworkHandler.PayloadType<T> payloadType);

        /** Register one serverbound payload's codec and receiver. */
        <T> void registerServerbound(NetworkHandler.PayloadType<T> payloadType);
    }

    /** Registers the physical-client receiver after the loader has registered payload codecs. */
    @FunctionalInterface
    public interface ClientReceiverRegistrar {
        /** Register the receiver for one clientbound payload type. */
        <T> void register(NetworkHandler.PayloadType<T> payloadType);
    }

    private static BiConsumer<ServerPlayer, CustomPacketPayload> serverSender;
    private static Predicate<CustomPacketPayload.Type<?>> serverCapability;
    private static Consumer<CustomPacketPayload> clientSender;

    private NetworkTransport() {
    }

    /**
     * Install the loader's server-to-player send operation.
     *
     * @throws IllegalStateException if a server sender is already installed
     * @throws NullPointerException if {@code sender} is {@code null}
     */
    public static synchronized void installServerSender(
            BiConsumer<ServerPlayer, CustomPacketPayload> sender) {
        if (serverSender != null) {
            throw new IllegalStateException("Server network sender is already installed");
        }
        serverSender = Objects.requireNonNull(sender);
    }

    /**
     * Install the physical client's server-capability query and client-to-server send operation.
     *
     * @throws IllegalStateException if either client operation is already installed
     * @throws NullPointerException if either argument is {@code null}
     */
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
