package com.github.crittscott.somegoogly.picker;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side authorization and throttling for client-driven picker operations — the two halves of the
 * same gate, always checked together by the packet handlers.
 *
 * <p>Authorization is creative mode and nothing else: no extra op/permission/config check. A server that
 * hands out creative already trusts the player with world edits, and admins should know creative also
 * enables the picker. The creative checks in the client CLI and keyboard picker are UX only and never
 * trusted; packet handlers use {@link #creative} so unauthorized custom-payload spam cannot amplify into
 * server feedback packets.
 */
public final class PickerGate {

    public static final int SPAWN_ALL_COOLDOWN_TICKS = 200;

    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new HashMap<>();
    private static int lastSpawnAllTick = Integer.MIN_VALUE;

    private PickerGate() {
    }

    /** Whether {@code sender} may drive one picker request now: creative, and not already rate-limited this tick. */
    public static boolean creative(ServerPlayer sender) {
        return sender.isCreative() && allowCreativeRequest(sender);
    }

    /** Permit at most one creative picker request per player in one server tick. */
    public static boolean allowCreativeRequest(ServerPlayer player) {
        int now = player.serverLevel().getServer().getTickCount();
        Integer last = LAST_REQUEST_TICK.put(player.getUUID(), now);
        return last == null || last != now;
    }

    /** Arm a server-wide cooldown for the destructive bulk-spawn operation. */
    public static boolean allowSpawnAll(MinecraftServer server) {
        int now = server.getTickCount();
        if (lastSpawnAllTick != Integer.MIN_VALUE && now - lastSpawnAllTick < SPAWN_ALL_COOLDOWN_TICKS) {
            return false;
        }
        lastSpawnAllTick = now;
        return true;
    }

    public static void onPlayerLeft(UUID playerId) {
        LAST_REQUEST_TICK.remove(playerId);
    }

    public static void onServerStopping() {
        LAST_REQUEST_TICK.clear();
        lastSpawnAllTick = Integer.MIN_VALUE;
    }
}
