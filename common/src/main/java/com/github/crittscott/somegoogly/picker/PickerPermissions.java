package com.github.crittscott.somegoogly.picker;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Server-side authorization for picker requests: the sender must be in <b>creative mode</b>.
 * Deliberately no extra op/permission/config gate — a server that hands out creative is already
 * trusting the player with world edits, and admins should know creative also enables the picker.
 *
 * <p>The creative checks in the client CLI and keyboard picker are UX only and never trusted. Packet
 * handlers use this authoritative, silently rejecting, rate-limited check so unauthorized custom
 * payload spam cannot amplify into server feedback packets.
 */
public final class PickerPermissions {

    private PickerPermissions() {
    }

    /** Whether {@code sender} may drive one picker request now. */
    public static boolean creative(@Nullable ServerPlayer sender) {
        return sender != null && sender.isCreative() && PickerRequestLimiter.allowCreativeRequest(sender);
    }
}
