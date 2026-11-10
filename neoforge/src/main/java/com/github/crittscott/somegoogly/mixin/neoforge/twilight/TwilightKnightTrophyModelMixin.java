package com.github.crittscott.somegoogly.mixin.neoforge.twilight;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Draws a Knight Phantom trophy's eyes on its helmet, right after the helmet is drawn, while the trophy draw is
 * bracketed by {@code TwilightTrophyRendererMixin} or {@code TwilightHeadItemRendererMixin}. Twilight Forest is an
 * optional mod with no API for this, and its trophy models draw themselves, so no vanilla head path or render layer
 * sees the part the eyes sit on.
 */
@Pseudo
@Mixin(targets = {"twilightforest.client.model.entity.KnightPhantomModel"}, remap = false)
abstract class TwilightKnightTrophyModelMixin {
    @Shadow private ModelPart helmet;

    @Redirect(method = "renderTrophy", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void somegoogly$drawEyes(ModelPart part, PoseStack pose, VertexConsumer consumer, int light,
                                    int overlay, int color, PoseStack trophyPose, MultiBufferSource buffers) {
        part.render(pose, consumer, light, overlay, color);
        if (part == helmet) {
            SkullEyeRenderer.renderTrophy(pose, buffers, light, part);
        }
    }
}
