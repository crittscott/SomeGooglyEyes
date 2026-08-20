package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.platform.fabric.FabricEntityPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies Fabric entities with the persistent compound Forge adds to its patched Entity class. */
@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements FabricEntityPersistentDataHolder {

    @Unique
    private static final String SOMEGOOGLY_DATA_KEY = "somegoogly:persistentData";

    @Unique
    private CompoundTag somegoogly$persistentData = new CompoundTag();

    @Override
    public CompoundTag somegoogly$getPersistentData() {
        return somegoogly$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void somegoogly$savePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
        if (!somegoogly$persistentData.isEmpty()) {
            tag.put(SOMEGOOGLY_DATA_KEY, somegoogly$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void somegoogly$loadPersistentData(CompoundTag tag, CallbackInfo callback) {
        somegoogly$persistentData = tag.contains(SOMEGOOGLY_DATA_KEY, Tag.TAG_COMPOUND)
                ? tag.getCompound(SOMEGOOGLY_DATA_KEY).copy()
                : new CompoundTag();
    }
}
