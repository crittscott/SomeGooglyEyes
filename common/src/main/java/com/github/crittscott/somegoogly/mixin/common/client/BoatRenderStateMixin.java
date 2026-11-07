package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** One boat frame's extracted eyes, overwritten on every extraction. */
@Mixin(BoatRenderState.class)
abstract class BoatRenderStateMixin implements PresentationEyes.Holder {

    @Unique
    private PresentationEyes.Draw somegoogly$draw = PresentationEyes.Draw.NONE;

    @Override
    public PresentationEyes.Draw somegoogly$draw() {
        return somegoogly$draw;
    }

    @Override
    public void somegoogly$setDraw(PresentationEyes.Draw draw) {
        somegoogly$draw = draw;
    }
}
