package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.ItemEyes;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;

/** Carries the frame's selected animation through render-state extraction. */
@Mixin(ItemFrameRenderState.class)
abstract class ItemFrameRenderStateMixin implements ItemEyes.Holder {

    @Unique
    private ItemEyes.@Nullable FrameData somegoogly$frameEye;

    @Override
    public ItemEyes.@Nullable FrameData somegoogly$frameEye() {
        return somegoogly$frameEye;
    }

    @Override
    public void somegoogly$setFrameEye(ItemEyes.@Nullable FrameData data) {
        somegoogly$frameEye = data;
    }
}
