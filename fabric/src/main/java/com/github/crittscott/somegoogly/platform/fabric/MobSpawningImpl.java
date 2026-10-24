package com.github.crittscott.somegoogly.platform.fabric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/** Fabric implementation of the shared spawn-finalization boundary. */
public final class MobSpawningImpl {

    private MobSpawningImpl() {
    }

    public static boolean finalizeSpawn(Mob mob, ServerLevel level, MobSpawnType reason) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), reason, null);
        return true;
    }
}
