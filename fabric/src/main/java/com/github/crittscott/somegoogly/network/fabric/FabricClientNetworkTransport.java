package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkProtocol;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

/** Physical-client Fabric payload receivers; each decodes on the network thread and handles on the client thread. */
public final class FabricClientNetworkTransport {

    private FabricClientNetworkTransport() {
    }

    /** Register the client's login-query answer, its refusal of a server without the mod, and its payload receivers. */
    public static void register() {
        ClientLoginNetworking.registerGlobalReceiver(FabricNetworkTransport.HANDSHAKE,
                (client, handler, buffer, listenerAdder) -> CompletableFuture.completedFuture(
                        PacketByteBufs.create().writeUtf(NetworkProtocol.NETWORK_VERSION)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ClientPlayNetworking.canSend(PickerFreezePacket.ID)) {
                handler.getConnection().disconnect(Component.translatable("somegoogly.network.required_server"));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(EyeStatePacket.ID, (client, handler, buffer, sender) -> {
            EyeStatePacket packet = EyeStatePacket.read(buffer);
            client.execute(() -> ClientNetworkHandler.handleEyeState(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(EyeConfigSyncPacket.ID, (client, handler, buffer, sender) -> {
            EyeConfigSyncPacket packet = EyeConfigSyncPacket.read(buffer);
            client.execute(() -> ClientNetworkHandler.handleEyeConfigSync(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(EyeSettingsPacket.ID, (client, handler, buffer, sender) -> {
            EyeSettingsPacket packet = EyeSettingsPacket.read(buffer);
            client.execute(() -> ClientNetworkHandler.handleEyeSettings(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(EyeBehaviorTriggerPacket.ID, (client, handler, buffer, sender) -> {
            EyeBehaviorTriggerPacket packet = EyeBehaviorTriggerPacket.read(buffer);
            client.execute(() -> ClientNetworkHandler.handleBehavior(packet));
        });
        ClientPlayNetworking.registerGlobalReceiver(EyeAngerPacket.ID, (client, handler, buffer, sender) -> {
            EyeAngerPacket packet = EyeAngerPacket.read(buffer);
            client.execute(() -> ClientNetworkHandler.handleAnger(packet));
        });
    }
}
