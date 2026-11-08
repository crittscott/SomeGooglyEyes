package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws hull-mounted eyes before vanilla restores the boat's composed model pose. Neither loader has a render event
 * for non-living entities that exposes that pose, and a render layer exists only for living entities.
 */
@Mixin(BoatRenderer.class)
abstract class BoatRendererMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void somegoogly$drawBoatEyes(Boat boat, float yaw, float partialTick, PoseStack pose,
                                         MultiBufferSource buffers, int light, CallbackInfo callback) {
        GooglyEyeRenderer.renderBoat(boat, partialTick, pose, buffers, light);
    }
}
