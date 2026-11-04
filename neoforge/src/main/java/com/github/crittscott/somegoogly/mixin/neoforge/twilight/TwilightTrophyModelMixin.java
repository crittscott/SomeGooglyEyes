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

@Pseudo
@Mixin(targets = {"twilightforest.client.model.entity.AlphaYetiModel",
        "twilightforest.client.model.entity.HydraHeadModel",
        "twilightforest.client.model.entity.LichModel",
        "twilightforest.client.model.entity.MinoshroomModel",
        "twilightforest.client.model.entity.NagaModel",
        "twilightforest.client.model.entity.QuestRamModel",
        "twilightforest.client.model.entity.SnowQueenModel",
        "twilightforest.client.model.entity.UrGhastModel"}, remap = false)
abstract class TwilightTrophyModelMixin {

    @Redirect(method = "renderTrophy", at = @At(value = "INVOKE", ordinal = 0, 
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void somegoogly$drawEyes(ModelPart part, PoseStack pose, VertexConsumer consumer, int light,
                                    int overlay, int color, PoseStack trophyPose, MultiBufferSource buffers) {
        part.render(pose, consumer, light, overlay, color);
        SkullEyeRenderer.renderTrophy(pose, buffers, light, part);
    }
}
