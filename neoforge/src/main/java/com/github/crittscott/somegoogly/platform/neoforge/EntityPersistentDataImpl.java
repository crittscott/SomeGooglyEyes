package com.github.crittscott.somegoogly.platform.neoforge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** NeoForge implementation of the shared entity-persistence boundary. */
public final class EntityPersistentDataImpl {

    private EntityPersistentDataImpl() {
    }

    public static CompoundTag get(Entity entity) {
        return entity.getPersistentData();
    }
}
