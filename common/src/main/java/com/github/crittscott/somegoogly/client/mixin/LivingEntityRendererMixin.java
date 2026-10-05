package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.EyeRenderData;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Takes each living entity's eye decision while its render state is extracted. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void somegoogly$extractEyeData(LivingEntity entity, LivingEntityRenderState state, float partialTick,
                                           CallbackInfo callback) {
        ((EyeRenderData.Holder) state).somegoogly$setEyeData(EyeRenderData.extract(entity, partialTick));
    }
}
