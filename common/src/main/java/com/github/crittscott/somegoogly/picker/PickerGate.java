package com.github.crittscott.somegoogly.picker;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
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
 *
 * <p>Every picker throttle lives here: one request per player per tick, the server-wide spawn-all
 * cooldown, and the two export limits. Each successful export triggers a full datapack reload, so
 * successes have the longer {@link #EXPORT_COOLDOWN_TICKS}; every attempt has the short
 * {@link #EXPORT_ATTEMPT_COOLDOWN_TICKS}, applied before any codec work. Per-player state is released
 * when the player leaves and all of it at server stop, since {@code getTickCount} restarts from 0 with
 * the next (single-player) world and a stale large tick would read as a far-future cooldown.
 */
public final class PickerGate {

    public static final int SPAWN_ALL_COOLDOWN_TICKS = 200;

    /** Ticks between successful exports per player (10 seconds; failed validation doesn't arm it). */
    public static final int EXPORT_COOLDOWN_TICKS = 200;

    /** Cheap attempt throttle applied before codec work, including malformed requests. */
    public static final int EXPORT_ATTEMPT_COOLDOWN_TICKS = 20;

    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new HashMap<>();
    private static final Map<UUID, Integer> LAST_EXPORT_ATTEMPT_TICK = new HashMap<>();
    private static final Map<UUID, Integer> LAST_EXPORT_TICK = new HashMap<>();
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

    /**
     * Arm the export attempt throttle and check both export limits. Returns {@code null} when the export
     * may proceed, or the feedback explaining why it may not.
     */
    @Nullable
    public static Component tryExport(MinecraftServer server, UUID playerId) {
        int now = server.getTickCount();
        Integer lastAttempt = LAST_EXPORT_ATTEMPT_TICK.get(playerId);
        if (lastAttempt != null && now - lastAttempt < EXPORT_ATTEMPT_COOLDOWN_TICKS) {
            return Component.translatable("somegoogly.command.picker.export_attempt_cooldown");
        }
        LAST_EXPORT_ATTEMPT_TICK.put(playerId, now);
        Integer last = LAST_EXPORT_TICK.get(playerId);
        if (last != null && now - last < EXPORT_COOLDOWN_TICKS) {
            int seconds = (EXPORT_COOLDOWN_TICKS - (now - last) + 19) / 20;
            return Component.translatable(seconds == 1
                    ? "somegoogly.command.picker.export_cooldown_one_second"
                    : "somegoogly.command.picker.export_cooldown_many_seconds", seconds);
        }
        return null;
    }

    /** Arm the success cooldown after an export was written. */
    public static void exportSucceeded(MinecraftServer server, UUID playerId) {
        LAST_EXPORT_TICK.put(playerId, server.getTickCount());
    }

    public static void onPlayerLeft(UUID playerId) {
        LAST_REQUEST_TICK.remove(playerId);
        LAST_EXPORT_ATTEMPT_TICK.remove(playerId);
        LAST_EXPORT_TICK.remove(playerId);
    }

    public static void onServerStopping() {
        LAST_REQUEST_TICK.clear();
        LAST_EXPORT_ATTEMPT_TICK.clear();
        LAST_EXPORT_TICK.clear();
        lastSpawnAllTick = Integer.MIN_VALUE;
    }
}
