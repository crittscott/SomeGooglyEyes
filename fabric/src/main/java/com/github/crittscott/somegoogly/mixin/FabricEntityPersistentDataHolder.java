package com.github.crittscott.somegoogly.mixin;

import net.minecraft.nbt.CompoundTag;

/** Implemented on every Fabric entity by {@link EntityPersistentDataMixin}. */
public interface FabricEntityPersistentDataHolder {

    CompoundTag somegoogly$getPersistentData();
}
