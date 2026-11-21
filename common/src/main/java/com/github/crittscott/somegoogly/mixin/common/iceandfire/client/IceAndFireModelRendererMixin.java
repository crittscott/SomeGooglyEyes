package com.github.crittscott.somegoogly.mixin.common.iceandfire.client;

import com.github.crittscott.somegoogly.client.compat.IceAndFireCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws eyes on Ice and Fire CE's cockatrice. Its renderer is built directly on {@code EntityRenderer}, so it has
 * none of the legacy-renderer features {@link IceAndFireCompat} adds to Ice and Fire's other mobs, and Ice and Fire,
 * an optional mod, offers no other hook. Its {@code submit} submits the model once, through
 * {@code LegacyMobRenderer.ModelPose.submit}, so the eyes follow that call while the pose stack still holds the model
 * pose and the shared model is posed for this entity.
 */
@Pseudo
@Mixin(targets = "com.iafenvoy.iceandfire.render.entity.CockatriceEntityRenderer", remap = false)
abstract class IceAndFireModelRendererMixin {

    @Inject(method = "submit(Lcom/iafenvoy/iceandfire/render/entity/LegacyEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/iafenvoy/iceandfire/render/entity/LegacyMobRenderer$ModelPose;submit",
                    shift = At.Shift.AFTER))
    private void somegoogly$submitEyes(@Coerce LivingEntityRenderState state, PoseStack poseStack,
                                       SubmitNodeCollector collector, CameraRenderState camera,
                                       CallbackInfo callback) {
        IceAndFireCompat.submitAfterModel((EntityRenderer<?, ?>) (Object) this, state, poseStack, collector);
    }
}
