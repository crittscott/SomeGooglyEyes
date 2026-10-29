package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no drop-collection event, and a mob's head reaches the world two ways the death-loot hook in
 * {@link LivingEntityDeathLootMixin} doesn't see: a charged-creeper kill spawns it after that hook runs, and a
 * loot-table head (the wither skeleton's) never passes through it. Both end in {@code spawnAtLocation}, so a
 * dying living entity's drops are offered to {@link EyeItemService#markHeadDrop} there, the counterpart of
 * Forge's {@code LivingDropsEvent} collection.
 */
@Mixin(Entity.class)
abstract class EntityHeadDropMixin {

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"))
    private void somegoogly$markHeadDrop(ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> callback) {
        if ((Object) this instanceof LivingEntity living && living.isDeadOrDying() && !living.level().isClientSide()) {
            EyeItemService.markHeadDrop(living, stack);
        }
    }
}
