package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.SkullEyes;
import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/** Carries a placed head's eyes from render-state extraction to its submission. */
@Mixin(SkullBlockRenderState.class)
abstract class SkullBlockRenderStateMixin implements SkullEyeRenderer.PlacedHolder {

    @Unique
    @Nullable
    private SkullEyes somegoogly$eyes;
    @Unique
    private float somegoogly$partialTick;

    @Override
    @Nullable
    public SkullEyes somegoogly$eyes() {
        return somegoogly$eyes;
    }

    @Override
    public float somegoogly$partialTick() {
        return somegoogly$partialTick;
    }

    @Override
    public void somegoogly$setEyes(@Nullable SkullEyes eyes, float partialTick) {
        somegoogly$eyes = eyes;
        somegoogly$partialTick = partialTick;
    }
}
