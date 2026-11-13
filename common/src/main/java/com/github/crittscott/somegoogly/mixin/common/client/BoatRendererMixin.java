package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries a boat's eyes through render-state extraction and submits them in {@code submit} before vanilla restores
 * the composed hull pose. No loader has a render event for non-living entities that exposes that pose, and a render
 * layer exists only for living entities.
 */
@Mixin(AbstractBoatRenderer.class)
abstract class BoatRendererMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/BoatRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void somegoogly$drawBoatEyes(BoatRenderState state, PoseStack pose, SubmitNodeCollector buffers,
                                         CameraRenderState camera, CallbackInfo callback) {
        GooglyEyeRenderer.renderBoat(state, pose, buffers, state.lightCoords);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;"
            + "Lnet/minecraft/client/renderer/entity/state/BoatRenderState;F)V", at = @At("TAIL"))
    private void somegoogly$extractBoatEyes(AbstractBoat boat, BoatRenderState state, float partialTick,
                                            CallbackInfo callback) {
        ((PresentationEyes.Holder) state).somegoogly$setDraw(
                PresentationEyes.Draw.of(ClientEyeRuntime.boatEyes(boat), partialTick));
    }
}
