package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Physical-client Fabric payload receivers. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EyeStatePacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleEyeState(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeConfigSyncPacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload));
        ClientPlayNetworking.registerGlobalReceiver(EyeBehaviorTriggerPacket.TYPE,
                (payload, context) -> ClientNetworkHandler.handleBehavior(payload));
    }
}
