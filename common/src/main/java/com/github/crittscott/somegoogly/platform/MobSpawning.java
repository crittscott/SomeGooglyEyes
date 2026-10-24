package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

/**
 * Runs a mob's spawn finalization the way the loader's own spawn paths do, so other mods' finalize-spawn
 * listeners see mobs the mod spawns. NeoForge and Forge route through their finalize-spawn event, which a
 * listener may cancel; Fabric has no such event and calls {@link Mob#finalizeSpawn} directly.
 */
public final class MobSpawning {

    private MobSpawning() {
    }

    /** Finalize {@code mob} at its current position; false when a listener cancelled the spawn. */
    @ExpectPlatform
    public static boolean finalizeSpawn(Mob mob, ServerLevel level, EntitySpawnReason reason) {
        throw new AssertionError();
    }
}
