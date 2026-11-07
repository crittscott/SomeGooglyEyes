package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Holds the stack an item render state was filled from ({@link SkullEyeRenderer.ItemHolder}) and the animation
 * chosen for it ({@link PresentationEyes.Holder}), and brackets the state's draw with them.
 */
@Mixin(ItemStackRenderState.class)
abstract class ItemStackRenderStateMixin implements SkullEyeRenderer.ItemHolder, PresentationEyes.Holder {

    @Unique
    private PresentationEyes.Draw somegoogly$draw = PresentationEyes.Draw.NONE;

    @Unique
    @Nullable
    private ItemStack somegoogly$item;

    @Inject(method = "render", at = @At("HEAD"))
    private void somegoogly$beginItem(PoseStack pose, MultiBufferSource bufferSource, int packedLight,
                                      int packedOverlay, CallbackInfo callback) {
        ClientEyeRuntime.beginItemDraw(somegoogly$draw);
        SkullEyeRenderer.beginItem(somegoogly$item);
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void somegoogly$clear(CallbackInfo callback) {
        somegoogly$item = null;
        somegoogly$draw = PresentationEyes.Draw.NONE;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void somegoogly$endItem(PoseStack pose, MultiBufferSource bufferSource, int packedLight,
                                    int packedOverlay, CallbackInfo callback) {
        SkullEyeRenderer.end();
        ClientEyeRuntime.endItemDraw();
    }

    @Override
    public PresentationEyes.Draw somegoogly$draw() {
        return somegoogly$draw;
    }

    @Override
    @Nullable
    public ItemStack somegoogly$item() {
        return somegoogly$item;
    }

    @Override
    public void somegoogly$setDraw(PresentationEyes.Draw draw) {
        somegoogly$draw = draw;
    }

    @Override
    public void somegoogly$setItem(@Nullable ItemStack stack) {
        somegoogly$item = stack;
    }
}
