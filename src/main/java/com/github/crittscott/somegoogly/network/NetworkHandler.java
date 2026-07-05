package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    // PROTOCOL_VERSION precedes INSTANCE by necessity: INSTANCE's initializer reads it by simple name,
    // and a forward reference there is a compile error (JLS 8.3.3) even though it is a constant.
    //
    // Version contract: mod versions may differ between client and server (the accept-all DisplayTest
    // in SomeGoogly allows it); this protocol version is what gates network compatibility. Bump it on
    // any breaking wire-format change so older clients are refused with a clear mismatch instead of
    // misparsing packets.
    private static final String PROTOCOL_VERSION = "6";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SomeGoogly.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        // Every packet declares its direction so Forge rejects copies sent the wrong way instead of
        // decoding and handling them. For the server→client sync packets that stops a (possibly
        // hostile) client burning server CPU on config-JSON decodes or, on a LAN host (physical
        // client), running the handlers' client branches against the host's world view.
        INSTANCE.registerMessage(
                id++,
                EyeStatePacket.class,
                EyeStatePacket::encode,
                EyeStatePacket::decode,
                EyeStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(
                id++,
                EyeConfigSyncPacket.class,
                EyeConfigSyncPacket::encode,
                EyeConfigSyncPacket::decode,
                EyeConfigSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        INSTANCE.registerMessage(
                id++,
                EyeBehaviorTriggerPacket.class,
                EyeBehaviorTriggerPacket::encode,
                EyeBehaviorTriggerPacket::decode,
                EyeBehaviorTriggerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Client→server picker requests (freeze/spawn/pose/export). Authorization happens in the
        // handlers — every one requires the sender to be in creative mode (PickerPermissions); the
        // client's own checks are UX only and never trusted.
        INSTANCE.registerMessage(
                id++,
                PickerFreezePacket.class,
                PickerFreezePacket::encode,
                PickerFreezePacket::decode,
                PickerFreezePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                PickerSpawnPacket.class,
                PickerSpawnPacket::encode,
                PickerSpawnPacket::decode,
                PickerSpawnPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                PickerSpawnAllPacket.class,
                PickerSpawnAllPacket::encode,
                PickerSpawnAllPacket::decode,
                PickerSpawnAllPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                PickerMobPosePacket.class,
                PickerMobPosePacket::encode,
                PickerMobPosePacket::decode,
                PickerMobPosePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        INSTANCE.registerMessage(
                id++,
                PickerExportPacket.class,
                PickerExportPacket::encode,
                PickerExportPacket::decode,
                PickerExportPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}