package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Holds the stack an item render state was filled from ({@link SkullEyeRenderer.ItemHolder}) and brackets the
 * state's submission with it, so a head item's eyes are submitted when its layer reaches the head-model submission.
 */
@Mixin(ItemStackRenderState.class)
abstract class ItemStackRenderStateMixin implements SkullEyeRenderer.ItemHolder {

    @Unique
    @Nullable
    private ItemStack somegoogly$item;

    @Override
    @Nullable
    public ItemStack somegoogly$item() {
        return somegoogly$item;
    }

    @Override
    public void somegoogly$setItem(@Nullable ItemStack stack) {
        somegoogly$item = stack;
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void somegoogly$clear(CallbackInfo callback) {
        somegoogly$item = null;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void somegoogly$beginItem(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                      int packedOverlay, int outlineColor, CallbackInfo callback) {
        SkullEyeRenderer.beginItem(somegoogly$item);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void somegoogly$endItem(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                    int packedOverlay, int outlineColor, CallbackInfo callback) {
        SkullEyeRenderer.end();
    }
}
