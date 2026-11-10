package com.github.crittscott.somegoogly.mixin.neoforge.twilight.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
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

/**
 * Brackets Twilight Forest's item renderer so a held, worn, framed, or GUI trophy or skull-candle item draws the
 * eyes its stack carries, as {@link SkullEyeRenderer} does for vanilla head items. Twilight Forest is an optional
 * mod with no API for this, and its items draw through its own renderer rather than vanilla's head item path.
 */
@Pseudo
@Mixin(targets = "twilightforest.client.ISTER", remap = false)
abstract class TwilightHeadItemRendererMixin {
    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void somegoogly$begin(ItemStack stack, ItemDisplayContext display, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        ClientEyeRuntime.beginHeadItem(stack);
    }

    @Inject(method = "renderByItem", at = @At("RETURN"))
    private void somegoogly$end(ItemStack stack, ItemDisplayContext display, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        ClientEyeRuntime.endHeadItem(stack);
    }
}
