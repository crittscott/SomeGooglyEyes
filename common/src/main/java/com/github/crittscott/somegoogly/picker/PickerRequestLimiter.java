package com.github.crittscott.somegoogly.picker;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread request throttles for client-driven picker operations. */
public final class PickerRequestLimiter {

    public static final int SPAWN_ALL_COOLDOWN_TICKS = 200;

    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new HashMap<>();
    private static int lastSpawnAllTick = Integer.MIN_VALUE;

    private PickerRequestLimiter() {
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
