package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
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
 *       releases the player's previous one, logout ({@link #unfreeze}) releases it too, and
 *       server stop ({@link #onServerStopping}) restores everything synchronously before the final
 *       save. A mob already frozen by another player is refused, since a second capture would read the
 *       forced value as the original. A frozen mob that converts into another ({@link #onConverted})
 *       hands its record and marker to its replacement.</li>
 *   <li><b>Persistent marker</b> — {@link #PREV_NO_AI_TAG} on the mob holds the pre-picker value for
 *       as long as the freeze is live, so an autosave + hard crash (or the mob's chunk unloading
 *       mid-edit) can't strand the forced {@code NoAi}: on the mob's next join
 *       ({@link #onMobJoin}) a stale marker is restored and stripped, while a still-live edit is
 *       re-asserted.</li>
 * </ul>
 *
 * <p>All state is transient and single-threaded: every entry point runs on the server thread, reached
 * from packet handlers and server lifecycle events.
 */
public final class PickerFreezeService {

    /**
     * Persistent marker holding a frozen mob's pre-picker NoAi value (crash/unload recovery). The
     * namespace is written out, not composed from the mod id: a persisted key must stay stable.
     */
    public static final String PREV_NO_AI_TAG = "somegoogly:pickerPrevNoAi";

    private static final Map<UUID, FrozenRecord> FROZEN_BY_PLAYER = new HashMap<>();

    /** One player's frozen mob, and the NoAi value to give back. */
    private record FrozenRecord(UUID mobId, boolean prevNoAi) {
    }

    private PickerFreezeService() {
    }

    /**
     * Freeze {@code mobId} in {@code level} for {@code playerId}, releasing any mob that player had
     * frozen before. Returns localized error feedback, and keeps the player's current freeze, when the mob is
     * already being edited by someone else; otherwise {@code null} — including the silent no-op cases (not
     * found in this level, or not a {@link Mob}; non-mob living entities have no AI to freeze).
     */
    @Nullable
    public static Component freeze(ServerLevel level, UUID playerId, UUID mobId) {
        for (Map.Entry<UUID, FrozenRecord> entry : FROZEN_BY_PLAYER.entrySet()) {
            if (!entry.getKey().equals(playerId) && entry.getValue().mobId().equals(mobId)) {
                return Component.translatable(
                        "somegoogly.command.picker.freeze_conflict", nameOf(level.getServer(), entry.getKey()));
            }
        }
        unfreeze(level.getServer(), playerId); // release the player's previous mob, if any

        Entity entity = level.getEntity(mobId);
        if (!(entity instanceof Mob mob)) {
            return null;
        }

        CompoundTag data = EntityPersistentData.get(mob);
        // Prefer a stale marker over the live flag: if a crash left the mob forced-NoAi with its marker
        // intact (and onMobJoin hasn't run for it), the marker holds the true pre-picker value.
        boolean prevNoAi = data.contains(PREV_NO_AI_TAG) ? data.getBooleanOr(PREV_NO_AI_TAG, false) : mob.isNoAi();
        data.putBoolean(PREV_NO_AI_TAG, prevNoAi);
        mob.setNoAi(true);
        mob.setDeltaMovement(Vec3.ZERO);
        FROZEN_BY_PLAYER.put(playerId, new FrozenRecord(mobId, prevNoAi));
        return null;
    }

    /** The mob {@code playerId} currently has frozen, or {@code null} if they have none. */
    @Nullable
    public static UUID frozenMobId(UUID playerId) {
        FrozenRecord record = FROZEN_BY_PLAYER.get(playerId);
        return record == null ? null : record.mobId();
    }

    /** Release {@code playerId}'s frozen mob (restore NoAi + strip the marker), if there is one. */
    public static void unfreeze(MinecraftServer server, UUID playerId) {
        FrozenRecord record = FROZEN_BY_PLAYER.remove(playerId);
        if (record != null) {
            restore(server, record);
        }
    }

    /** The record's mob in whichever level it is in now, since it may have gone through a portal. */
    private static void restore(MinecraftServer server, FrozenRecord record) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(record.mobId()) instanceof Mob mob) {
                mob.setNoAi(record.prevNoAi());
                EntityPersistentData.get(mob).remove(PREV_NO_AI_TAG);
                return;
            }
        }
        // Not loaded (chunk unloaded mid-edit): the marker was saved with the mob, so onMobJoin
        // restores it on next load. A dead/removed mob simply has nothing to restore.
    }

    /**
     * {@code source}, about to be replaced by {@code target} (lightning, curing, a mooshroom's shearing),
     * hands its freeze to the replacement: vanilla carries the forced {@code NoAi} across but not the marker,
     * and the record names the source's UUID, so otherwise the replacement could never be released. A
     * replacement that isn't a {@link Mob} has no AI to hold, so the freeze simply ends.
     */
    public static void onConverted(LivingEntity source, LivingEntity target) {
        UUID sourceId = source.getUUID();
        for (Map.Entry<UUID, FrozenRecord> entry : FROZEN_BY_PLAYER.entrySet()) {
            FrozenRecord record = entry.getValue();
            if (!record.mobId().equals(sourceId)) {
                continue;
            }
            if (target instanceof Mob mob) {
                EntityPersistentData.get(mob).putBoolean(PREV_NO_AI_TAG, record.prevNoAi());
                mob.setNoAi(true);
                entry.setValue(new FrozenRecord(mob.getUUID(), record.prevNoAi()));
            } else {
                FROZEN_BY_PLAYER.remove(entry.getKey());
            }
            return;
        }
    }

    /**
     * A mob (re)joined a server level carrying the freeze marker: if some player's live record still
     * claims it, re-assert the freeze (its chunk reloaded mid-edit); otherwise the marker is stale
     * (crash, or an unfreeze that found it unloaded) — restore the pre-picker value and strip it.
     */
    public static void onMobJoin(Mob mob) {
        CompoundTag data = EntityPersistentData.get(mob);
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
        SomeGooglyCommon.LOGGER.info("Restoring AI state of {} {} left frozen by the picker",
                BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()), mobId);
        mob.setNoAi(data.getBooleanOr(PREV_NO_AI_TAG, false));
        data.remove(PREV_NO_AI_TAG);
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

    private static String nameOf(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player != null ? player.getGameProfile().name() : playerId.toString();
    }
}
