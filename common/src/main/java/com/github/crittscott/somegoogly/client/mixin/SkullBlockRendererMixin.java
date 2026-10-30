package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * No loader has a block-entity renderer hook, so placed heads' eyes ride vanilla's head renderer: the
 * placed-head draw is bracketed for {@link SkullEyeRenderer}, which draws the eyes right after the shared
 * model draw.
 */
@Mixin(SkullBlockRenderer.class)
abstract class SkullBlockRendererMixin {

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"))
    private void somegoogly$beginHead(SkullBlockEntity skull, float partialTick, PoseStack pose,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                      CallbackInfo callback) {
        SkullEyeRenderer.begin(skull, partialTick);
    }

    @Inject(method = "renderSkull", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private static void somegoogly$drawEyes(@Nullable Direction direction, float yRot, float mouthAnimation,
                                            PoseStack pose, MultiBufferSource bufferSource, int packedLight,
                                            SkullModelBase model, RenderType renderType, CallbackInfo callback) {
        SkullEyeRenderer.render(pose, bufferSource, packedLight, model);
    }

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("RETURN"))
    private void somegoogly$endHead(SkullBlockEntity skull, float partialTick, PoseStack pose,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                    CallbackInfo callback) {
        SkullEyeRenderer.end();
    }
}
