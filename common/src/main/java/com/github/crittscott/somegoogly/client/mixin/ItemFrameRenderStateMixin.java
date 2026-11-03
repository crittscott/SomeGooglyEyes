package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;

/** Carries the frame's selected animation through render-state extraction. */
@Mixin(ItemFrameRenderState.class)
abstract class ItemFrameRenderStateMixin implements PresentationEyes.Holder {

    @Unique
    private PresentationEyes.@Nullable FrameData somegoogly$frameEye;

    @Override
    public PresentationEyes.@Nullable FrameData somegoogly$frameEye() {
        return somegoogly$frameEye;
    }

    @Override
    public void somegoogly$setFrameEye(PresentationEyes.@Nullable FrameData data) {
        somegoogly$frameEye = data;
    }
}
