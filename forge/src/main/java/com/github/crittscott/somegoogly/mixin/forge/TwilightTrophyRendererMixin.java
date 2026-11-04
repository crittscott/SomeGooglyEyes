package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.Model;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "twilightforest.client.renderer.tileentity.TrophyTileEntityRenderer", remap = false)
abstract class TwilightTrophyRendererMixin {
    private static final String PLACED_RENDER = "render(Ltwilightforest/block/entity/TrophyBlockEntity;"
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

    @Inject(method = "render(Lnet/minecraft/core/Direction;FLtwilightforest/client/model/tileentity/GenericTrophyModel;"
            + "Ltwilightforest/enums/BossVariant;FLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ItemDisplayContext;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", remap = true))
    private static void somegoogly$drawEyes(Direction direction, float yaw, @Coerce Model model,
                                           @Coerce Object variant, float animation, PoseStack pose,
                                           MultiBufferSource buffers, int light, ItemDisplayContext display,
                                           CallbackInfo callback) {
        // For the Knight Phantom, the current pose is the helmet's and the eyes attach to it.
        SkullEyeRenderer.renderTrophy(pose, buffers, light, model);
    }
}
