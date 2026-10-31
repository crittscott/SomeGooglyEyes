package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.migration.fabric.FabricEntityDataMigration;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands each loading entity's saved data to {@link FabricEntityDataMigration}. */
@Mixin(Entity.class)
abstract class EntityPersistentDataMigrationMixin {

    @Inject(method = "load", at = @At("TAIL"))
    private void somegoogly$migratePersistentData(ValueInput input, CallbackInfo callback) {
        FabricEntityDataMigration.migrate((Entity) (Object) this, input);
    }
}
