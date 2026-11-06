package com.github.crittscott.somegoogly.network.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Required Forge payload channel and native packet distribution. The channel's version is the network
 * version, and both sides require it, so Forge refuses a client or server without a matching mod.
 */
public final class ForgeNetworkTransport {

    /** The payload channel; each packet's number below is its wire id, so it stays fixed whatever the order. */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SomeGooglyCommon.MOD_ID, "network"),
            () -> NetworkHandler.NETWORK_VERSION,
            NetworkHandler.NETWORK_VERSION::equals,
            NetworkHandler.NETWORK_VERSION::equals);

    private ForgeNetworkTransport() {
    }

    /** Register every payload; called once from the mod constructor, before any send. */
    public static void register() {
        // Lambda bodies, not method references, so a dedicated server never links the client handler.
        CHANNEL.messageBuilder(EyeStatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeStatePacket::write)
                .decoder(EyeStatePacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleEyeState(payload))
                .add();
        CHANNEL.messageBuilder(EyeConfigSyncPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeConfigSyncPacket::write)
                .decoder(EyeConfigSyncPacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload))
                .add();
        CHANNEL.messageBuilder(EyeBehaviorTriggerPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeBehaviorTriggerPacket::write)
                .decoder(EyeBehaviorTriggerPacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleBehavior(payload))
                .add();
        CHANNEL.messageBuilder(EyeAngerPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeAngerPacket::write)
                .decoder(EyeAngerPacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleAnger(payload))
                .add();
        CHANNEL.messageBuilder(PickerFreezePacket.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickerFreezePacket::write)
                .decoder(PickerFreezePacket::read)
                .consumerMainThread((payload, context) -> PickerFreezePacket.handle(payload, sender(context)))
                .add();
        CHANNEL.messageBuilder(PickerExportPacket.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickerExportPacket::write)
                .decoder(PickerExportPacket::read)
                .consumerMainThread((payload, context) -> PickerExportPacket.handle(payload, sender(context)))
                .add();
        CHANNEL.messageBuilder(EyeSettingsPacket.class, 6, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeSettingsPacket::write)
                .decoder(EyeSettingsPacket::read)
                .consumerMainThread((payload, context) -> ClientNetworkHandler.handleEyeSettings(payload))
                .add();
    }

    private static ServerPlayer sender(Supplier<NetworkEvent.Context> context) {
        return context.get().getSender();
    }
}
