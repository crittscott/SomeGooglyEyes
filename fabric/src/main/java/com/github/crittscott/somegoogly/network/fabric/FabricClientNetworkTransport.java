package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

/** Physical-client Fabric payload receivers. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    /** Register the client's version answer, its refusal of a server without the mod, and its payload receivers. */
    public static void register() {
        ClientConfigurationNetworking.registerGlobalReceiver(FabricNetworkTransport.Handshake.TYPE,
                (payload, context) -> context.responseSender().sendPacket(
                        new FabricNetworkTransport.HandshakeReply(NetworkProtocol.NETWORK_VERSION)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ClientPlayNetworking.canSend(PickerFreezePacket.TYPE)) {
                handler.getConnection().disconnect(Component.translatable("somegoogly.network.required_server"));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(EyeStatePacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleEyeState(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeConfigSyncPacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeSettingsPacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleEyeSettings(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeBehaviorTriggerPacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleBehavior(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeAngerPacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleAnger(payload));
    }
}
