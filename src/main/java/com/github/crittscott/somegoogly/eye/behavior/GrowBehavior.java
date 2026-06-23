package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.HeadInfo;

/** Eyes briefly bulge larger and settle back. Drives the eye-scale channel. */
final class GrowBehavior extends AbstractEyeBehavior {

    private static final float GROW = 0.5f; // extra scale at the peak

    GrowBehavior() {
        super("grow", 14);
    }

    @Override
    public void tick(BehaviorInstance i) {
        i.prevScale = i.scale;
        i.scale = 1f + GROW * Curves.sinPulse((float) i.age / i.duration);
    }

    @Override
    public void contribute(BehaviorInstance i, HeadInfo helper, int head, int eye, float pt, EyeRenderContribution out) {
        out.eyeScaleMul = Curves.lerp(i.prevScale, i.scale, pt);
    }
}
