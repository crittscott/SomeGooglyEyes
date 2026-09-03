package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.picker.PickerPermissions;
import com.github.crittscott.somegoogly.picker.PickerRequestLimiter;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server-side authorization and rate limits behind the client-driven picker verbs:
 * {@link PickerPermissions#creative} (a null sender is rejected; the rest is a plain
 * {@code isCreative()} check left to source review) and {@link PickerRequestLimiter} (one request per
 * player per tick, plus a server-wide cooldown on the destructive bulk spawn).
 * {@code ConfigGameTestsLogic.spawnAllDefaultsOff} pins the opt-in config default; these pin the
 * silent-rejection gates that keep unauthorized custom-payload spam from amplifying into server work.
 */
public final class PickerAuthGameTestsLogic {

    private PickerAuthGameTestsLogic() {
    }

    /** A null sender is rejected outright, and a valid sender gets at most one request per tick. */
    public static void pickerRequestsRequireCreativeAndThrottlePerTick(GameTestHelper helper, ServerPlayer player) {
        PickerRequestLimiter.onPlayerLeft(player.getUUID());
        try {
            helper.assertTrue(!PickerPermissions.creative(null), "a null sender is rejected");

            helper.assertTrue(PickerRequestLimiter.allowCreativeRequest(player),
                    "the first request in a tick is allowed");
            helper.assertTrue(!PickerRequestLimiter.allowCreativeRequest(player),
                    "a second request in the same tick is refused");

            PickerRequestLimiter.onPlayerLeft(player.getUUID());
            helper.assertTrue(PickerRequestLimiter.allowCreativeRequest(player),
                    "clearing the player's record lets the next request through");
        } finally {
            PickerRequestLimiter.onPlayerLeft(player.getUUID());
        }
        helper.succeed();
    }

    /** {@code allowSpawnAll} arms a server-wide cooldown; a second call inside the window is refused. */
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        PickerRequestLimiter.onServerStopping();
        try {
            helper.assertTrue(PickerRequestLimiter.allowSpawnAll(server), "the first spawn-all is allowed");
            helper.assertTrue(!PickerRequestLimiter.allowSpawnAll(server),
                    "a second spawn-all inside the cooldown is refused");

            PickerRequestLimiter.onServerStopping();
            helper.assertTrue(PickerRequestLimiter.allowSpawnAll(server),
                    "the cooldown resets when picker state is cleared");
        } finally {
            PickerRequestLimiter.onServerStopping();
        }
        helper.succeed();
    }
}
