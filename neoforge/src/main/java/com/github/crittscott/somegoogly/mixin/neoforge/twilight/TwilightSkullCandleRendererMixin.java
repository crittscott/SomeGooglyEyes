package com.github.crittscott.somegoogly.mixin.neoforge.twilight;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Brackets a placed skull candle's draw and draws its eyes after the head model, as
 * {@code SkullBlockRendererMixin} does for vanilla heads. Twilight Forest is an optional mod with no API for
 * this, and its skull candle renderer draws the head itself instead of going through vanilla's head renderer.
 */
@Pseudo
@Mixin(targets = "twilightforest.client.renderer.block.SkullCandleRenderer", remap = false)
abstract class TwilightSkullCandleRendererMixin {
    private static final String PLACED_RENDER = "render(Ltwilightforest/block/entity/SkullCandleBlockEntity;"
            + "FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V";

    @Inject(method = PLACED_RENDER, at = @At("HEAD"))
    private void somegoogly$begin(@Coerce BlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        SkullEyeRenderer.begin(entity, partialTick);
    }

    @Inject(method = PLACED_RENDER, at = @At("RETURN"))
    private void somegoogly$end(CallbackInfo callback) {
        SkullEyeRenderer.end();
    }

    @Inject(method = "renderSkull", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private static void somegoogly$drawEyes(Direction direction, float yaw, float animation, PoseStack pose,
                                           MultiBufferSource buffers, int light, SkullModelBase model,
                                           RenderType type, CallbackInfo callback) {
        SkullEyeRenderer.render(pose, buffers, light, model);
    }
}
