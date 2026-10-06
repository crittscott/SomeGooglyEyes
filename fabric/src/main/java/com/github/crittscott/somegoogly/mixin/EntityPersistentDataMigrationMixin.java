package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.platform.fabric.EntityPersistentDataImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Migrates entity data saved by the previous released Fabric build, which stored the mod's persistent
 * compound under the {@code somegoogly:persistentData} entity-tag key, into the persistent-data attachment
 * when the entity loads. The key is never written again.
 */
@Mixin(Entity.class)
abstract class EntityPersistentDataMigrationMixin {

    @Unique
    private static final String RELEASED_KEY = "somegoogly:persistentData";

    @Inject(method = "load", at = @At("TAIL"))
    private void somegoogly$migratePersistentData(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(RELEASED_KEY, Tag.TAG_COMPOUND)) {
            ((Entity) (Object) this).setAttached(EntityPersistentDataImpl.TYPE, tag.getCompound(RELEASED_KEY).copy());
        }
    }
}
