package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric hook for the reaction to a player's hit, the point Forge's {@code LivingDamageEvent} marks. Fabric API's
 * {@code ALLOW_DAMAGE} fires before invulnerability and damage are resolved. {@code Player} overrides
 * {@code actuallyHurt} without calling super, so both classes are targeted.
 */
@Mixin({LivingEntity.class, Player.class})
abstract class HurtReactionMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void somegoogly$afterHurt(DamageSource source, float amount, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (source.getEntity() instanceof Player) {
            ServerBehaviorScheduler.onPlayerHurt(self);
        }
    }
}
