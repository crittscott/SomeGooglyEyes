package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric hooks for the hurt/heal reactions that have no equivalent Fabric API event. */
@Mixin(LivingEntity.class)
abstract class LivingEntityReactionMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void somegoogly$afterHurt(DamageSource source, float amount, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && source.getEntity() instanceof Player) {
            ServerBehaviorScheduler.onPlayerHurt(self);
        }
    }

    @Inject(method = "heal", at = @At("TAIL"))
    private void somegoogly$afterHeal(float amount, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            ServerBehaviorScheduler.onHealed(self);
        }
    }
}
