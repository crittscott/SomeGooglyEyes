package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.server.EyeItemService;
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
 * Fabric has no drop-collection event, and a mob's head reaches the world two ways the death-loot hook in
 * {@link LivingEntityDeathLootMixin} doesn't see: a charged-creeper kill spawns it after that hook runs, and a
 * loot-table head (the wither skeleton's) never passes through it. Both end in {@code spawnAtLocation}, so a
 * dying living entity's drops are offered to {@link EyeItemService#markHeadDrop} there, the counterpart of
 * Forge's {@code LivingDropsEvent} collection. Both offset forms are hooked, so a drop is seen whichever one a
 * caller uses; a stack passing through both is stamped once.
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
