package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric hook for the heal reaction, which has no equivalent Fabric API event. */
@Mixin(LivingEntity.class)
abstract class LivingEntityReactionMixin {

    @Inject(method = "heal", at = @At("TAIL"))
    private void somegoogly$afterHeal(float amount, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            ServerBehaviorScheduler.onHealed(self);
        }
    }
}
