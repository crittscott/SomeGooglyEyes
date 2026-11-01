package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.BoatEyeRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** One boat frame's extracted eye data, overwritten on every extraction. */
@Mixin(BoatRenderState.class)
abstract class BoatRenderStateMixin implements BoatEyeRenderer.Holder {

    @Unique
    private BoatEyeRenderer.Data somegoogly$boatEyes = BoatEyeRenderer.Data.NONE;

    @Override
    public BoatEyeRenderer.Data somegoogly$boatEyes() {
        return somegoogly$boatEyes;
    }

    @Override
    public void somegoogly$setBoatEyes(BoatEyeRenderer.Data data) {
        somegoogly$boatEyes = data;
    }
}
