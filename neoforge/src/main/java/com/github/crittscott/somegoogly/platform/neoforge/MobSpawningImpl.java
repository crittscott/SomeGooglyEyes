package com.github.crittscott.somegoogly.platform.neoforge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.EventHooks;

/** NeoForge implementation of the shared spawn-finalization boundary, through {@code FinalizeSpawnEvent}. */
public final class MobSpawningImpl {

    private MobSpawningImpl() {
    }

    public static boolean finalizeSpawn(Mob mob, ServerLevel level, EntitySpawnReason reason) {
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), reason, null);
        return !mob.isSpawnCancelled();
    }
}
