package com.github.crittscott.somegoogly.mixin.neoforge.twilight;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import org.jspecify.annotations.Nullable;

/** Carries a placed head's eyes from render-state extraction to its submission. */
@Pseudo
@Mixin(targets = "twilightforest.client.state.block.TrophyRenderState", remap = false)
abstract class TwilightTrophyRenderStateMixin implements SkullEyeRenderer.PlacedHolder {

    @Unique
    @Nullable
    private PresentationEyes somegoogly$eyes;
    @Unique
    private float somegoogly$partialTick;

    @Override
    @Nullable
    public PresentationEyes somegoogly$eyes() {
        return somegoogly$eyes;
    }

    @Override
    public float somegoogly$partialTick() {
        return somegoogly$partialTick;
    }

    @Override
    public void somegoogly$setEyes(@Nullable PresentationEyes eyes, float partialTick) {
        somegoogly$eyes = eyes;
        somegoogly$partialTick = partialTick;
    }
}
