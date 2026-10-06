package com.github.crittscott.somegoogly.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Physical-client receive handoff. Loader payload registration runs on both distributions, so its
 * clientbound handlers forward here instead of naming client classes; the client installs the receiver.
 */
public final class NetworkTransport {

    private static Consumer<CustomPacketPayload> clientReceiver;

    private NetworkTransport() {
    }

    public static synchronized void installClientReceiver(Consumer<CustomPacketPayload> receiver) {
        if (clientReceiver != null) {
            throw new IllegalStateException("Client network receiver is already installed");
        }
        clientReceiver = Objects.requireNonNull(receiver);
    }

    public static void receiveClientbound(CustomPacketPayload payload) {
        if (clientReceiver == null) {
            throw new IllegalStateException("Client network receiver is not installed");
        }
        clientReceiver.accept(payload);
    }
}
