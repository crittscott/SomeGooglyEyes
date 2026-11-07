package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.item.EyeItemService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no drop-collection event. Both charged-creeper and loot-table heads pass through
 * {@code spawnAtLocation}, the counterpart of Forge and NeoForge's {@code LivingDropsEvent} collection.
 * Hook both offset forms; a stack passing through both is stamped once.
 */
@Mixin(Entity.class)
abstract class EntityHeadDropMixin {

    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"))
    private void somegoogly$markHeadDrop(ServerLevel level, ItemStack stack, float offsetY,
                                         CallbackInfoReturnable<ItemEntity> callback) {
        somegoogly$offer(stack);
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"))
    private void somegoogly$markHeadDropAtOffset(ServerLevel level, ItemStack stack, Vec3 offset,
                                                 CallbackInfoReturnable<ItemEntity> callback) {
        somegoogly$offer(stack);
    }

    @Unique
    private void somegoogly$offer(ItemStack stack) {
        if ((Object) this instanceof LivingEntity living && living.isDeadOrDying()) {
            EyeItemService.markHeadDrop(living, stack);
        }
    }
}
