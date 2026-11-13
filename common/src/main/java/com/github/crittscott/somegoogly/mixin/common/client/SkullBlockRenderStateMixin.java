package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.jspecify.annotations.Nullable;

/**
 * Carries a placed head's eyes from render-state extraction to its submission. Vanilla render states have no slot
 * for another mod's data, and no loader offers one, so the eyes ride the state as mixin fields.
 */
@Mixin(SkullBlockRenderState.class)
abstract class SkullBlockRenderStateMixin implements SkullEyeRenderer.PlacedHolder {

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
