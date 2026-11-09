package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One of the mod's payloads: its id and its body. Each payload class pairs this with a static
 * {@code read(FriendlyByteBuf)} that validates on decode; the loader bridges
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking} send it on that id.
 *
 * <p>{@link #CLIENTBOUND} and {@link #SERVERBOUND} are the one declaration of every payload, its direction, and
 * what handles it on arrival; each loader registers from them. Clientbound handlers reach
 * {@link ClientNetworkHandler} only inside lambda bodies, so a dedicated server never links it.
 */
public interface Payload {

    /** Every payload the server sends, with its client-thread handler. */
    List<Clientbound<?>> CLIENTBOUND = List.of(
            new Clientbound<>(0, EyeStatePacket.class, EyeStatePacket.ID, EyeStatePacket::read,
                    packet -> ClientNetworkHandler.handleEyeState(packet)),
            new Clientbound<>(1, EyeConfigSyncPacket.class, EyeConfigSyncPacket.ID, EyeConfigSyncPacket::read,
                    packet -> ClientNetworkHandler.handleEyeConfigSync(packet)),
            new Clientbound<>(2, EyeBehaviorTriggerPacket.class, EyeBehaviorTriggerPacket.ID,
                    EyeBehaviorTriggerPacket::read, packet -> ClientNetworkHandler.handleBehavior(packet)),
            new Clientbound<>(3, EyeAngerPacket.class, EyeAngerPacket.ID, EyeAngerPacket::read,
                    packet -> ClientNetworkHandler.handleAnger(packet)),
            new Clientbound<>(6, EyeSettingsPacket.class, EyeSettingsPacket.ID, EyeSettingsPacket::read,
                    packet -> ClientNetworkHandler.handleEyeSettings(packet)));

    /** Every payload a client sends, with its server-thread handler, which re-checks the sender's authority. */
    List<Serverbound<?>> SERVERBOUND = List.of(
            new Serverbound<>(4, PickerFreezePacket.class, PickerFreezePacket.ID, PickerFreezePacket::read,
                    PickerFreezePacket::handle),
            new Serverbound<>(5, PickerExportPacket.class, PickerExportPacket.ID, PickerExportPacket::read,
                    PickerExportPacket::handle));

    /** The channel id this payload is sent on. */
    ResourceLocation id();

    /** Encode this payload's body. */
    void write(FriendlyByteBuf buffer);

    /**
     * A payload the server sends. {@code index} is its fixed wire number on loaders that number payloads,
     * unique across both directions.
     */
    record Clientbound<T extends Payload>(int index, Class<T> type, ResourceLocation id,
                                          Function<FriendlyByteBuf, T> reader, Consumer<T> handler) {
    }

    /**
     * A payload a client sends. {@code index} is its fixed wire number on loaders that number payloads,
     * unique across both directions.
     */
    record Serverbound<T extends Payload>(int index, Class<T> type, ResourceLocation id,
                                          Function<FriendlyByteBuf, T> reader, BiConsumer<T, ServerPlayer> handler) {
    }
}
