package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.mixin.FabricEntityPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Fabric implementation of the shared entity-persistence boundary. */
public final class EntityPersistentDataImpl {

    private EntityPersistentDataImpl() {
    }

    public static CompoundTag get(Entity entity) {
        return ((FabricEntityPersistentDataHolder) entity).somegoogly$getPersistentData();
    }
}
