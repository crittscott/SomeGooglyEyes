package com.github.crittscott.somegoogly.network.neoforge;

import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Required NeoForge payload registration and server receivers. */
public final class NeoForgeNetworkTransport {

    private NeoForgeNetworkTransport() {
    }

    /** Register the payloads when the mod bus fires its payload event; called once from the mod constructor. */
    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeNetworkTransport::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NetworkHandler.NETWORK_VERSION);
        registrar.playToClient(EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeState(payload));
        registrar.playToClient(EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleEyeConfigSync(payload));
        registrar.playToClient(EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC,
                (payload, context) -> ClientNetworkHandler.handleBehavior(payload));
        registrar.playToServer(PickerFreezePacket.TYPE, PickerFreezePacket.STREAM_CODEC,
                (payload, context) -> PickerFreezePacket.handle(payload, serverPlayer(context)));
        registrar.playToServer(PickerExportPacket.TYPE, PickerExportPacket.STREAM_CODEC,
                (payload, context) -> PickerExportPacket.handle(payload, serverPlayer(context)));
    }

    private static ServerPlayer serverPlayer(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            return player;
        }
        throw new IllegalStateException("Serverbound payload has no authenticated server player");
    }
}
