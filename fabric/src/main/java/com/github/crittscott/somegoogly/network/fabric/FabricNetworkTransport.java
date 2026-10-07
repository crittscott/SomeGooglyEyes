package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fabric payload codecs and server receivers. */
public final class FabricNetworkTransport {

    /**
     * Configuration-phase marker that is registered but never sent. A client that registers a receiver
     * for it declares that it runs this network version, which the server checks before the player joins.
     */
    public record Handshake() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Handshake> TYPE =
                new CustomPacketPayload.Type<>(NetworkHandler.versioned("handshake"));
        public static final StreamCodec<FriendlyByteBuf, Handshake> STREAM_CODEC = StreamCodec.unit(new Handshake());

        @Override
        public CustomPacketPayload.Type<Handshake> type() {
            return TYPE;
        }
    }

    private FabricNetworkTransport() {
    }

    public static void register() {
        PayloadTypeRegistry.configurationS2C().register(Handshake.TYPE, Handshake.STREAM_CODEC);
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
