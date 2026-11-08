package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Pseudo
@Mixin(targets = "twilightforest.client.ISTER", remap = false)
abstract class TwilightHeadItemRendererMixin {
    @Inject(method = {"renderByItem", "m_108829_"}, at = @At("HEAD"))
    private void somegoogly$begin(ItemStack stack, ItemDisplayContext display, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        SkullEyeRenderer.beginItem(stack);
    }

    @Inject(method = {"renderByItem", "m_108829_"}, at = @At("RETURN"))
    private void somegoogly$end(ItemStack stack, ItemDisplayContext display, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        SkullEyeRenderer.endItem(stack);
    }
}
