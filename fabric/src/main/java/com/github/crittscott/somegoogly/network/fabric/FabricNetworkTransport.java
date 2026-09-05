package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric payload codecs, server receivers, and direct server sends. */
public final class FabricNetworkTransport {

    private FabricNetworkTransport() {
    }

    public static void register() {
        NetworkTransport.installServerSender(ServerPlayNetworking::send);

        PayloadTypeRegistry.playS2C().register(EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PickerFreezePacket.TYPE,
                (payload, context) -> PickerFreezePacket.handle(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(PickerExportPacket.TYPE,
                (payload, context) -> PickerExportPacket.handle(payload, context.player()));
    }
}
