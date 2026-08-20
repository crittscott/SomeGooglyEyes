package com.github.crittscott.somegoogly.platform.fabric;

import net.minecraft.nbt.CompoundTag;

/** Implemented on every Fabric entity by the persistent-data Mixin. */
public interface FabricEntityPersistentDataHolder {

    CompoundTag somegoogly$getPersistentData();
}
