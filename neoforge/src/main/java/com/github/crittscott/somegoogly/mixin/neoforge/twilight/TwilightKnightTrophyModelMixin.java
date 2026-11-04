package com.github.crittscott.somegoogly.mixin.neoforge.twilight;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(targets = {"twilightforest.client.model.entity.KnightPhantomModel"}, remap = false)
abstract class TwilightKnightTrophyModelMixin {
    @ModifyArgs(method = "renderTrophy", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart"))
    private void somegoogly$submitEyes(Args args, PoseStack pose, SubmitNodeCollector collector, int light,
                                        int overlay, ModelFeatureRenderer.CrumblingOverlay crumbling,
                                        ItemDisplayContext display) {
        if (args.size() == 6) {
            SkullEyeRenderer.renderTrophy(args.get(1), collector, args.get(3), args.get(0));
        }
    }
}
