package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.picker.PickerGate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server-side authorization and rate limits behind the client-driven picker verbs:
 * {@link PickerGate#creative} (a null sender is rejected; the rest is a plain
 * {@code isCreative()} check left to source review) and the {@link PickerGate} rate limits (one request
 * per player per tick, plus a server-wide cooldown on the destructive bulk spawn).
 * {@code ConfigGameTestsLogic.spawnAllDefaultsOff} pins the opt-in config default; these pin the
 * silent-rejection gates that keep unauthorized custom-payload spam from amplifying into server work.
 */
public final class PickerAuthGameTestsLogic {

    private PickerAuthGameTestsLogic() {
    }

    /** A null sender is rejected outright, and a valid sender gets at most one request per tick. */
    public static void pickerRequestsRequireCreativeAndThrottlePerTick(GameTestHelper helper, ServerPlayer player) {
        PickerGate.onPlayerLeft(player.getUUID());
        try {
            helper.assertTrue(!PickerGate.creative(null), "a null sender is rejected");

            helper.assertTrue(PickerGate.allowCreativeRequest(player),
                    "the first request in a tick is allowed");
            helper.assertTrue(!PickerGate.allowCreativeRequest(player),
                    "a second request in the same tick is refused");

            PickerGate.onPlayerLeft(player.getUUID());
            helper.assertTrue(PickerGate.allowCreativeRequest(player),
                    "clearing the player's record lets the next request through");
        } finally {
            PickerGate.onPlayerLeft(player.getUUID());
        }
        helper.succeed();
    }

    /** {@code allowSpawnAll} arms a server-wide cooldown; a second call inside the window is refused. */
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        PickerGate.onServerStopping();
        try {
            helper.assertTrue(PickerGate.allowSpawnAll(server), "the first spawn-all is allowed");
            helper.assertTrue(!PickerGate.allowSpawnAll(server),
                    "a second spawn-all inside the cooldown is refused");

            PickerGate.onServerStopping();
            helper.assertTrue(PickerGate.allowSpawnAll(server),
                    "the cooldown resets when picker state is cleared");
        } finally {
            PickerGate.onServerStopping();
        }
        helper.succeed();
    }
}
