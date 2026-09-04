package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric has no drop-collection event, so the shears-on-kill harvest rides {@code dropCustomDeathLoot}
 * — the same death-loot phase NeoForge and Forge reach through {@code LivingDropsEvent} — instead of a
 * bare post-death {@code spawnAtLocation}. {@link EyeItemService#onDeath} applies its own killer, weapon,
 * and chance guards.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityDeathLootMixin {

    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void somegoogly$harvestEyeOnKill(ServerLevel level, DamageSource damageSource,
                                             boolean recentlyHit, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        EyeItemService.onDeath(self, damageSource, self::spawnAtLocation);
    }
}
