package com.github.crittscott.somegoogly.network.neoforge;

import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Required NeoForge payload registration and native packet sends. */
public final class NeoForgeNetworkTransport {

    private NeoForgeNetworkTransport() {
    }

    public static void register(IEventBus modBus) {
        NetworkTransport.installServerSender(PacketDistributor::sendToPlayer);
        modBus.addListener(NeoForgeNetworkTransport::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NetworkHandler.NETWORK_VERSION);
        registrar.playToClient(EyeStatePacket.TYPE, EyeStatePacket.STREAM_CODEC,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        registrar.playToClient(EyeConfigSyncPacket.TYPE, EyeConfigSyncPacket.STREAM_CODEC,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
        registrar.playToClient(EyeBehaviorTriggerPacket.TYPE, EyeBehaviorTriggerPacket.STREAM_CODEC,
                (payload, context) -> NetworkTransport.receiveClientbound(payload));
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
