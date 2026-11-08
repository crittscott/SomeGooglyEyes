package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.migration.fabric.FabricMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands each loading entity's tag to {@link FabricMigration} once the entity has read it. Fabric has no event for an
 * entity loading its saved data.
 */
@Mixin(Entity.class)
abstract class EntityPersistentDataMigrationMixin {

    @Inject(method = "load", at = @At("TAIL"))
    private void somegoogly$migratePersistentData(CompoundTag tag, CallbackInfo callback) {
        FabricMigration.migrateEntity((Entity) (Object) this, tag);
    }
}
