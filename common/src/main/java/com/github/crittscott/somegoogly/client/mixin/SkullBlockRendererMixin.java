package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * No loader has a block-entity renderer hook, so placed heads' eyes ride vanilla's head renderer: their eyes are
 * chosen as the head's render state is extracted, and the placed-head submission is bracketed for
 * {@link SkullEyeRenderer}, which submits the eyes right after the shared model submission.
 */
@Mixin(SkullBlockRenderer.class)
abstract class SkullBlockRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void somegoogly$extractEyes(SkullBlockEntity skull, SkullBlockRenderState state, float partialTick,
                                        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling,
                                        CallbackInfo callback) {
        ((SkullEyeRenderer.PlacedHolder) state).somegoogly$setEyes(SkullEyeRenderer.extract(skull), partialTick);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"))
    private void somegoogly$beginHead(SkullBlockRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                      CameraRenderState camera, CallbackInfo callback) {
        SkullEyeRenderer.begin((SkullEyeRenderer.PlacedHolder) state);
    }

    // The model is submitted in the skull's own pose, which is popped right after.
    @Inject(method = "submitSkull", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private static void somegoogly$drawEyes(@Nullable Direction direction, float yRot, float mouthAnimation,
                                            PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                            SkullModelBase model, RenderType renderType, int outlineColor,
                                            @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling,
                                            CallbackInfo callback) {
        SkullEyeRenderer.render(pose, collector, packedLight, model, yRot, mouthAnimation);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("RETURN"))
    private void somegoogly$endHead(SkullBlockRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo callback) {
        SkullEyeRenderer.end();
    }
}
