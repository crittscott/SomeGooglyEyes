package com.github.crittscott.somegoogly.picker;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Server-side authorization for picker requests: the sender must be in <b>creative mode</b>.
 * Deliberately no extra op/permission/config gate — a server that hands out creative is already
 * trusting the player with world edits, and admins should know creative also enables the picker.
 *
 * <p>The creative checks in the client CLI ({@code GooglyClientCommands}) and keyboard picker are
 * UX only and never trusted; this is the authoritative check, applied by every client→server picker
 * packet handler ({@code PickerFreezePacket}, {@code PickerSpawnPacket}, {@code PickerSpawnAllPacket},
 * {@code PickerMobPosePacket}, {@code PickerExportPacket}).
 */
public final class PickerPermissions {

    private PickerPermissions() {
    }

    /** Whether {@code sender} may drive the picker; messages the player and returns false otherwise. */
    public static boolean creative(@Nullable ServerPlayer sender) {
        if (sender == null) {
            return false;
        }
        if (sender.isCreative()) {
            return true;
        }
        sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.requires_creative"));
        return false;
    }
}
