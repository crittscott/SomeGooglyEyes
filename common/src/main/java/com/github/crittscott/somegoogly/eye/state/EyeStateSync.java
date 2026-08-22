package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sends coherent eye-state snapshots through the cross-loader network, either to one newly tracking
 * player or to the entity's complete tracking set.
 */
public final class EyeStateSync {

    private EyeStateSync() {
    }

    public static void sendTo(LivingEntity entity, ServerPlayer player) {
        NetworkHandler.sendEyeState(player, packet(entity, EyeState.snapshot(entity)));
    }

    public static void sync(LivingEntity entity, EyeState.Snapshot snapshot) {
        NetworkHandler.sendEyeStateTrackingAndSelf(entity, packet(entity, snapshot));
    }

    private static EyeStatePacket packet(LivingEntity entity, EyeState.Snapshot snapshot) {
        return new EyeStatePacket(entity.getId(), snapshot);
    }
}
