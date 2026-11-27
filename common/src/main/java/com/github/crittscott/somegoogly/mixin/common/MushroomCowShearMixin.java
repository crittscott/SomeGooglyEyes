package com.github.crittscott.somegoogly.mixin.common;

import com.github.crittscott.somegoogly.server.ServerServices;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Shearing a mooshroom builds its replacement cow by hand rather than through {@code
 * Mob#convertTo}, so neither loader's mob-conversion event fires for it. This hands the mooshroom's
 * eyes (and any picker freeze) to that cow as it is added to the level, as the conversion listeners
 * do for every other conversion. Forge's 1.20.1 patch puts cow creation in its {@code
 * shearInternal} helper, while Fabric uses {@code shear}, so both are targeted.
 */
@Mixin(MushroomCow.class)
abstract class MushroomCowShearMixin {

    // Forge's 1.20.1 patch defines this helper, but common's compile target does not contain it.
    // The verified descriptor supplies the type information the processor cannot infer.
    // Mixin invokes this handler.
    @SuppressWarnings({"target", "UnusedMethod"})
    @ModifyArg(
            method = {"shear", "shearInternal(Lnet/minecraft/sounds/SoundSource;)Ljava/util/List;"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity somegoogly$keepEyesOnShear(Entity spawned) {
        if (spawned instanceof Cow cow) {
            ServerServices.onConverted((MushroomCow) (Object) this, cow);
        }
        return spawned;
    }
}
