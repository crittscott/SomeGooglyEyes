package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.Payload;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Required Forge payload channel and native packet distribution. The channel's version is the network
 * version, and both sides require it, so Forge refuses a client or server without a matching mod.
 */
public final class ForgeNetworkTransport {

    /** The payload channel; each payload's {@code index} in {@link Payload} is its wire id here. */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            SomeGooglyCommon.id("network"),
            () -> NetworkProtocol.NETWORK_VERSION,
            NetworkProtocol.NETWORK_VERSION::equals,
            NetworkProtocol.NETWORK_VERSION::equals);

    private ForgeNetworkTransport() {
    }

    /** Register every payload; called once from the mod constructor, before any send. */
    public static void register() {
        for (Payload.Clientbound<?> payload : Payload.CLIENTBOUND) {
            registerClientbound(payload);
        }
        for (Payload.Serverbound<?> payload : Payload.SERVERBOUND) {
            registerServerbound(payload);
        }
    }

    private static <T extends Payload> void registerClientbound(Payload.Clientbound<T> payload) {
        CHANNEL.messageBuilder(payload.type(), payload.index(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Payload::write)
                .decoder(payload.reader())
                .consumerMainThread((packet, context) -> payload.handler().accept(packet))
                .add();
    }

    private static <T extends Payload> void registerServerbound(Payload.Serverbound<T> payload) {
        CHANNEL.messageBuilder(payload.type(), payload.index(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(Payload::write)
                .decoder(payload.reader())
                .consumerMainThread((packet, context) -> payload.handler().accept(packet, sender(context)))
                .add();
    }

    private static ServerPlayer sender(Supplier<NetworkEvent.Context> context) {
        return context.get().getSender();
    }
}
