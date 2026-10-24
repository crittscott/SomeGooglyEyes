package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Shearing a mooshroom builds its replacement cow by hand rather than through {@code Mob#convertTo}, so
 * Fabric API's mob-conversion event does not fire for it. This copies the mooshroom's eyes onto that cow as
 * it is added to the level, as the conversion listener does for every other conversion.
 */
@Mixin(MushroomCow.class)
abstract class MushroomCowShearMixin {

    @ModifyArg(method = "shear", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity somegoogly$keepEyesOnShear(Entity spawned) {
        if (spawned instanceof Cow cow) {
            EyeState.copy((MushroomCow) (Object) this, cow);
        }
        return spawned;
    }
}
