package com.github.crittscott.somegoogly.network.fabric;

import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.github.crittscott.somegoogly.network.PickerMobPosePacket;
import com.github.crittscott.somegoogly.network.PickerSpawnAllPacket;
import com.github.crittscott.somegoogly.network.PickerSpawnPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric server receivers; each decodes on the network thread and the handler applies on the server thread. */
public final class FabricNetworkTransport {

    private FabricNetworkTransport() {
    }

    /** Register the serverbound receivers; called once from the mod initializer. */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.PROTOCOL_ACK,
                (server, player, handler, buffer, sender) -> NetworkHandler.receiveAcknowledgement(buffer, player));
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.PICKER_FREEZE,
                (server, player, handler, buffer, sender) ->
                        PickerFreezePacket.handle(PickerFreezePacket.decode(buffer), player));
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.PICKER_SPAWN,
                (server, player, handler, buffer, sender) ->
                        PickerSpawnPacket.handle(PickerSpawnPacket.decode(buffer), player));
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.PICKER_SPAWN_ALL,
                (server, player, handler, buffer, sender) ->
                        PickerSpawnAllPacket.handle(PickerSpawnAllPacket.decode(buffer), player));
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.PICKER_MOB_POSE,
                (server, player, handler, buffer, sender) ->
                        PickerMobPosePacket.handle(PickerMobPosePacket.decode(buffer), player));
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.PICKER_EXPORT,
                (server, player, handler, buffer, sender) ->
                        PickerExportPacket.handle(PickerExportPacket.decode(buffer), player));
    }
}
