package com.github.crittscott.somegoogly.platform.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.ForgeEventFactory;

/** Forge implementation of the shared spawn-finalization boundary, through the finalize-spawn event. */
public final class MobSpawningImpl {

    private MobSpawningImpl() {
    }

    public static boolean finalizeSpawn(Mob mob, ServerLevel level, EntitySpawnReason reason) {
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), reason, null);
        return !mob.isSpawnCancelled();
    }
}
