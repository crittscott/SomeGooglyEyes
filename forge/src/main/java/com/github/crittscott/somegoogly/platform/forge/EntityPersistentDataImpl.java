package com.github.crittscott.somegoogly.platform.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Forge implementation of the shared entity-persistence boundary. */
public final class EntityPersistentDataImpl {

    private EntityPersistentDataImpl() {
    }

    public static CompoundTag get(Entity entity) {
        return entity.getPersistentData();
    }
}
