package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import javax.annotation.Nullable;

/** Carries the frame's selected animation through render-state extraction. */
@Mixin(ItemFrameRenderState.class)
abstract class ItemFrameRenderStateMixin implements PresentationEyes.Holder {

    @Unique
    @Nullable
    private PresentationEyes.FrameData somegoogly$frameEye;

    @Override
    @Nullable
    public PresentationEyes.FrameData somegoogly$frameEye() {
        return somegoogly$frameEye;
    }

    @Override
    public void somegoogly$setFrameEye(@Nullable PresentationEyes.FrameData data) {
        somegoogly$frameEye = data;
    }
}
