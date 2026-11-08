package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lightning builds the witch and the zombified piglin by hand rather than through {@code Mob#convertTo}, so
 * Fabric's mob-conversion event doesn't fire for them; Forge's does. This hands the struck mob's eyes (and any
 * picker freeze) to its replacement as it is added to the level. Each target class has only its own injection point.
 */
@Mixin({Pig.class, Villager.class})
abstract class LightningConversionMixin {

    @ModifyArg(method = "thunderHit", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity somegoogly$keepPigEyes(Entity converted) {
        ServerServices.onConverted((LivingEntity) (Object) this, (LivingEntity) converted);
        return converted;
    }

    @ModifyArg(method = "thunderHit", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private Entity somegoogly$keepVillagerEyes(Entity converted) {
        ServerServices.onConverted((LivingEntity) (Object) this, (LivingEntity) converted);
        return converted;
    }
}
