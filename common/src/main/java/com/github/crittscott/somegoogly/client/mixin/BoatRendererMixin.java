package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.BoatEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries boat eyes through extraction and draws before the composed hull pose is restored. */
@Mixin(AbstractBoatRenderer.class)
abstract class BoatRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/BoatRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void somegoogly$drawBoatEyes(BoatRenderState state, PoseStack pose, MultiBufferSource buffers,
                                         int light, CallbackInfo callback) {
        BoatEyeRenderer.render(state, pose, buffers, light);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/vehicle/AbstractBoat;"
            + "Lnet/minecraft/client/renderer/entity/state/BoatRenderState;F)V", at = @At("TAIL"))
    private void somegoogly$extractBoatEyes(AbstractBoat boat, BoatRenderState state, float partialTick,
                                            CallbackInfo callback) {
        ((BoatEyeRenderer.Holder) state).somegoogly$setBoatEyes(BoatEyeRenderer.extract(boat, partialTick));
    }
}
