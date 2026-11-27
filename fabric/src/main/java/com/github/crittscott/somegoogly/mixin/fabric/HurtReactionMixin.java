package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric hook for the damage reactions, the point NeoForge's and Forge's {@code LivingDamageEvent}
 * marks. Fabric API's {@code ALLOW_DAMAGE} fires before invulnerability and damage are resolved.
 * {@code Player} overrides {@code actuallyHurt} without calling super, so both classes are
 * targeted. A shield-blocked hit still reaches {@code actuallyHurt}, with no damage, so the
 * reaction needs some.
 */
@Mixin({LivingEntity.class, Player.class})
abstract class HurtReactionMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void somegoogly$afterHurt(DamageSource source, float amount, CallbackInfo callback) {
        // Only a hit with damage is a hurt: one a shield blocked arrives here with none.
        if (amount > 0.0F) {
            ServerBehaviorScheduler.onHurt((LivingEntity) (Object) this, source);
        }
    }
}
