package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.item.EyeItemService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric has no drop-collection event, so the shears-on-kill harvest runs at the end of {@code dropAllDeathLoot},
 * where NeoForge and Forge post {@code LivingDropsEvent}, whatever vanilla decided about the mob's own loot.
 * {@link EyeItemService#onDeath} applies its own game-rule, killer, weapon, and chance guards.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityDeathLootMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void somegoogly$harvestEyeOnKill(ServerLevel level, DamageSource damageSource, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        EyeItemService.onDeath(self, damageSource, stack -> self.spawnAtLocation(level, stack));
    }
}
