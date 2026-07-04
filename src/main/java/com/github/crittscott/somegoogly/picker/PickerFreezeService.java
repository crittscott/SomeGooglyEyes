package com.github.crittscott.somegoogly.picker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-owned picker mob freezing: a picker-targeted mob is held {@code NoAi=true} while edited and
 * restored to its pre-picker value when released. Owning this on the server (reached via
 * {@code PickerFreezePacket}) is what lets a remote client use the picker — and what guarantees the
 * forced flag is undone even when that client disappears.
 *
 * <p>Robustness, in layers:
 * <ul>
 *   <li><b>Live records</b> — one per editing player ({@link #FROZEN_BY_PLAYER}); freezing a new mob
 *       releases the player's previous one, logout ({@link #onPlayerLoggedOut}) releases it too, and
 *       server stop ({@link #onServerStopping}) restores everything synchronously before the final
 *       save. A mob already frozen by another player is refused, since a second capture would read the
 *       forced value as the original.</li>
 *   <li><b>Persistent marker</b> — {@link #PREV_NO_AI_TAG} on the mob holds the pre-picker value for
 *       as long as the freeze is live, so an autosave + hard crash (or the mob's chunk unloading
 *       mid-edit) can't strand the forced {@code NoAi}: on the mob's next join
 *       ({@link #onMobJoin}) a stale marker is restored and stripped, while a still-live edit is
 *       re-asserted.</li>
 * </ul>
 *
 * <p>All state is transient and single-threaded: every entry point runs on the server thread (packet
 * {@code enqueueWork} / Forge lifecycle events).
 */
public final class PickerFreezeService {

    /** Persistent marker holding a frozen mob's pre-picker NoAi value (crash/unload recovery). */
    public static final String PREV_NO_AI_TAG = "somegoogly:pickerPrevNoAi";

    private static final Map<UUID, FrozenRecord> FROZEN_BY_PLAYER = new HashMap<>();

    /** One player's frozen mob: where it lives, which it is, and the NoAi value to give back. */
    private record FrozenRecord(ResourceKey<Level> dim, UUID mobId, boolean prevNoAi) {
    }

    private PickerFreezeService() {
    }

    /**
     * Freeze {@code mobId} in {@code level} for {@code playerId}, releasing any mob that player had
     * frozen before. Returns an error message when the mob is already being edited by someone else, or
     * {@code null} on success — including the silent no-op cases (not found in this level, or not a
     * {@link Mob}; non-mob living entities have no AI to freeze, matching the old client-side behavior).
     */
    @Nullable
    public static String freeze(ServerLevel level, UUID playerId, UUID mobId) {
        unfreeze(level.getServer(), playerId); // release the player's previous mob, if any

        for (Map.Entry<UUID, FrozenRecord> entry : FROZEN_BY_PLAYER.entrySet()) {
            if (entry.getValue().mobId().equals(mobId)) {
                return "That mob is already being edited by " + nameOf(level.getServer(), entry.getKey()) + ".";
            }
        }

        Entity entity = level.getEntity(mobId);
        if (!(entity instanceof Mob mob)) {
            return null;
        }

        CompoundTag data = mob.getPersistentData();
        // Prefer a stale marker over the live flag: if a crash left the mob forced-NoAi with its marker
        // intact (and onMobJoin hasn't run for it), the marker holds the true pre-picker value.
        boolean prevNoAi = data.contains(PREV_NO_AI_TAG) ? data.getBoolean(PREV_NO_AI_TAG) : mob.isNoAi();
        data.putBoolean(PREV_NO_AI_TAG, prevNoAi);
        mob.setNoAi(true);
        mob.setDeltaMovement(Vec3.ZERO);
        FROZEN_BY_PLAYER.put(playerId, new FrozenRecord(level.dimension(), mobId, prevNoAi));
        return null;
    }

    /** Release {@code playerId}'s frozen mob (restore NoAi + strip the marker), if there is one. */
    public static void unfreeze(MinecraftServer server, UUID playerId) {
        FrozenRecord record = FROZEN_BY_PLAYER.remove(playerId);
        if (record != null) {
            restore(server, record);
        }
    }

    private static void restore(MinecraftServer server, FrozenRecord record) {
        ServerLevel level = server.getLevel(record.dim());
        Entity entity = level == null ? null : level.getEntity(record.mobId());
        if (entity instanceof Mob mob) {
            mob.setNoAi(record.prevNoAi());
            mob.getPersistentData().remove(PREV_NO_AI_TAG);
        }
        // Not loaded (chunk unloaded mid-edit): the marker was saved with the mob, so onMobJoin
        // restores it on next load. A dead/removed mob simply has nothing to restore.
    }

    /**
     * A mob (re)joined a server level carrying the freeze marker: if some player's live record still
     * claims it, re-assert the freeze (its chunk reloaded mid-edit); otherwise the marker is stale
     * (crash, or an unfreeze that found it unloaded) — restore the pre-picker value and strip it.
     */
    public static void onMobJoin(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(PREV_NO_AI_TAG)) {
            return;
        }
        UUID mobId = mob.getUUID();
        for (FrozenRecord record : FROZEN_BY_PLAYER.values()) {
            if (record.mobId().equals(mobId)) {
                mob.setNoAi(true);
                return;
            }
        }
        mob.setNoAi(data.getBoolean(PREV_NO_AI_TAG));
        data.remove(PREV_NO_AI_TAG);
    }

    /** The disappearing-client hook: a leaving player's frozen mob is always released. */
    public static void onPlayerLoggedOut(ServerPlayer player) {
        unfreeze(player.serverLevel().getServer(), player.getUUID());
    }

    /**
     * Restore every frozen mob synchronously at server stop — before the final world save — so a
     * forced {@code NoAi} is never persisted by a clean shutdown (markers included).
     */
    public static void onServerStopping(MinecraftServer server) {
        for (FrozenRecord record : FROZEN_BY_PLAYER.values()) {
            restore(server, record);
        }
        FROZEN_BY_PLAYER.clear();
    }

    /**
     * Drop all live records <b>without restoring</b> — the state after a hard crash, which the marker
     * tags then recover from on next load ({@link #onMobJoin}). Exists for the gametests that pin that
     * recovery; real shutdown goes through {@link #onServerStopping}.
     */
    public static void clear() {
        FROZEN_BY_PLAYER.clear();
    }

    private static String nameOf(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player != null ? player.getGameProfile().getName() : playerId.toString();
    }
}
