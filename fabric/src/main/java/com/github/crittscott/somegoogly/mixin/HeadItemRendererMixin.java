package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Head items draw through vanilla's built-in item renderer, which reaches the same head-model draw as placed
 * heads; bracketing every item it draws lets {@link SkullEyeRenderer} give a head item its eyes. Forge carries
 * the same Mixin.
 */
@Mixin(BlockEntityWithoutLevelRenderer.class)
abstract class HeadItemRendererMixin {

    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void somegoogly$beginItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack pose,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                      CallbackInfo callback) {
        SkullEyeRenderer.beginItem(stack);
    }

    @Inject(method = "renderByItem", at = @At("RETURN"))
    private void somegoogly$endItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack pose,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                    CallbackInfo callback) {
        SkullEyeRenderer.end();
    }
}
