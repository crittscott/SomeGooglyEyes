package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.render.EyeRenderData;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Carries the eye decision from render-state extraction to the eye layers. Vanilla render states have no slot for
 * another mod's data, and no loader offers one, so the decision rides the state as a mixin field.
 */
@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements EyeRenderData.Holder {

    @Unique
    private EyeRenderData somegoogly$eyeData = EyeRenderData.NONE;

    @Override
    public EyeRenderData somegoogly$eyeData() {
        return somegoogly$eyeData;
    }

    @Override
    public void somegoogly$setEyeData(EyeRenderData data) {
        somegoogly$eyeData = data;
    }
}
