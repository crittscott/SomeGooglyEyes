package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.ItemEyes;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import javax.annotation.Nullable;

/** Carries the frame's selected animation through render-state extraction. */
@Mixin(ItemFrameRenderState.class)
abstract class ItemFrameRenderStateMixin implements ItemEyes.Holder {

    @Unique
    @Nullable
    private ItemEyes.FrameData somegoogly$frameEye;

    @Override
    @Nullable
    public ItemEyes.FrameData somegoogly$frameEye() {
        return somegoogly$frameEye;
    }

    @Override
    public void somegoogly$setFrameEye(@Nullable ItemEyes.FrameData data) {
        somegoogly$frameEye = data;
    }
}
