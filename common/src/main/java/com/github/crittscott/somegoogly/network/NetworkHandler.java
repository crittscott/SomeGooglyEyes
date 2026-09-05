package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.platform.NetworkTracking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** The mod's five payload ids and small loader-neutral send surface. */
public final class NetworkHandler {

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "11";

    public static final ResourceLocation EYE_STATE = versioned("eye_state");
    public static final ResourceLocation EYE_CONFIG = versioned("eye_config");
    public static final ResourceLocation EYE_BEHAVIOR = versioned("eye_behavior");
    public static final ResourceLocation PICKER_FREEZE = versioned("picker_freeze");
    public static final ResourceLocation PICKER_EXPORT = versioned("picker_export");

    private NetworkHandler() {
    }

    public static void sendConfig(ServerPlayer player) {
        NetworkTransport.sendToPlayer(player, new EyeConfigSyncPacket(ServerEyeConfigs.all()));
    }

    public static void sendEyeState(ServerPlayer player, EyeStatePacket packet) {
        NetworkTransport.sendToPlayer(player, packet);
    }

    public static void sendEyeStateTrackingAndSelf(Entity entity, EyeStatePacket packet) {
        NetworkTracking.send(entity, true, packet);
    }

    public static void sendBehavior(ServerPlayer player, EyeBehaviorTriggerPacket packet) {
        NetworkTransport.sendToPlayer(player, packet);
    }

    public static void sendBehaviorTracking(Entity entity, EyeBehaviorTriggerPacket packet) {
        NetworkTracking.send(entity, false, packet);
    }

    public static void sendToServer(PickerFreezePacket packet) {
        NetworkTransport.sendToServer(packet);
    }

    public static void sendToServer(PickerExportPacket packet) {
        NetworkTransport.sendToServer(packet);
    }

    private static ResourceLocation versioned(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                SomeGooglyCommon.MOD_ID, "v" + NETWORK_VERSION + "/" + path);
    }
}
