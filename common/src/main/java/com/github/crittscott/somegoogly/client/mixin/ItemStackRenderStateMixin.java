package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.GuiItemEyes;
import com.github.crittscott.somegoogly.client.HandItemEyes;
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

import org.jspecify.annotations.Nullable;

/**
 * Holds the stack an item render state was filled from ({@link SkullEyeRenderer.ItemHolder}) and brackets the
 * state's submission with it, so a head item's eyes are submitted when its layer reaches the head-model submission.
 */
@Mixin(ItemStackRenderState.class)
abstract class ItemStackRenderStateMixin implements SkullEyeRenderer.ItemHolder, GuiItemEyes.Holder, HandItemEyes.Holder {

    @Unique
    private GuiItemEyes.Context somegoogly$guiEyes = GuiItemEyes.EMPTY;

    @Unique
    private HandItemEyes.Context somegoogly$handEyes = HandItemEyes.EMPTY;

    @Unique
    @Nullable
    private ItemStack somegoogly$item;

    @Inject(method = "submit", at = @At("HEAD"))
    private void somegoogly$beginItem(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                      int packedOverlay, int outlineColor, CallbackInfo callback) {
        GuiItemEyes.beginRender(somegoogly$guiEyes);
        HandItemEyes.begin(somegoogly$handEyes);
        SkullEyeRenderer.beginItem(somegoogly$item);
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void somegoogly$clear(CallbackInfo callback) {
        somegoogly$item = null;
        somegoogly$guiEyes = GuiItemEyes.EMPTY;
        somegoogly$handEyes = HandItemEyes.EMPTY;
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void somegoogly$endItem(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                                    int packedOverlay, int outlineColor, CallbackInfo callback) {
        SkullEyeRenderer.end();
        HandItemEyes.end();
        GuiItemEyes.endDraw();
    }

    @Override
    public GuiItemEyes.Context somegoogly$guiEyes() {
        return somegoogly$guiEyes;
    }

    @Override
    public HandItemEyes.Context somegoogly$handEyes() {
        return somegoogly$handEyes;
    }

    @Override
    @Nullable
    public ItemStack somegoogly$item() {
        return somegoogly$item;
    }

    @Override
    public void somegoogly$setGuiEyes(GuiItemEyes.Context context) {
        somegoogly$guiEyes = context;
    }

    @Override
    public void somegoogly$setHandEyes(HandItemEyes.Context context) {
        somegoogly$handEyes = context;
    }

    @Override
    public void somegoogly$setItem(@Nullable ItemStack stack) {
        somegoogly$item = stack;
    }

}
