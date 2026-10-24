package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Required Forge payload channel and native packet distribution. The channel's version is the network
 * version, and both sides require it, so Forge refuses a client or server without a matching mod.
 */
public final class ForgeNetworkTransport {

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SomeGooglyCommon.MOD_ID, "network"),
            () -> NetworkHandler.NETWORK_VERSION,
            NetworkHandler.NETWORK_VERSION::equals,
            NetworkHandler.NETWORK_VERSION::equals);

    private ForgeNetworkTransport() {
    }

    /** Register every payload; called once from the mod constructor, before any send. */
    public static void register() {
        int index = 0;
        // Lambda bodies, not method references, so a dedicated server never links the client handler.
        CHANNEL.messageBuilder(EyeStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeStatePacket::write)
                .decoder(EyeStatePacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleEyeState(payload))
                .add();
        CHANNEL.messageBuilder(EyeConfigSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeConfigSyncPacket::write)
                .decoder(EyeConfigSyncPacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload))
                .add();
        CHANNEL.messageBuilder(EyeBehaviorTriggerPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeBehaviorTriggerPacket::write)
                .decoder(EyeBehaviorTriggerPacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleBehavior(payload))
                .add();
        CHANNEL.messageBuilder(PickerFreezePacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickerFreezePacket::write)
                .decoder(PickerFreezePacket::read)
                .consumerMainThread((payload, context) -> PickerFreezePacket.handle(payload, sender(context)))
                .add();
        CHANNEL.messageBuilder(PickerExportPacket.class, index, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickerExportPacket::write)
                .decoder(PickerExportPacket::read)
                .consumerMainThread((payload, context) -> PickerExportPacket.handle(payload, sender(context)))
                .add();
    }

    /** Send to one player. */
    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    /** Send from the client to the server. */
    public static void sendToServer(Payload payload) {
        CHANNEL.sendToServer(payload);
    }

    /** Send to every player tracking {@code entity}, and to the entity itself if it is a player. */
    public static void sendTrackingAndSelf(Entity entity, Payload payload) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), payload);
    }

    private static ServerPlayer sender(Supplier<NetworkEvent.Context> context) {
        return Objects.requireNonNull(context.get().getSender(), "Serverbound payload has no authenticated sender");
    }
}
