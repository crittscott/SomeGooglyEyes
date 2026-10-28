package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports a sheep being sheared, which Forge has no event for. Shears used by a player or a dispenser end
 * in {@code setSheared(true)} whether vanilla or Forge's {@code IForgeShearable#onSheared} does the
 * shearing, so its change from unsheared marks a shearing.
 */
@Mixin(Sheep.class)
abstract class ShearingMixin {

    @Inject(method = "setSheared", at = @At("HEAD"))
    private void somegoogly$onSheared(boolean sheared, CallbackInfo callback) {
        Sheep self = (Sheep) (Object) this;
        if (sheared && !self.isSheared() && !self.level().isClientSide()) {
            ServerBehaviorScheduler.onSheared(self);
        }
    }
}
