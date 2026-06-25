package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.HeadInfo;

/** Pupils glide to center and hold, then drift back to the physics wobble. Drives the iris channel. */
final class StareBehavior extends AbstractEyeBehavior {

    StareBehavior() {
        super("stare", 50);
    }

    @Override
    public void contribute(BehaviorInstance i, HeadInfo helper, int head, int eye, float pt, EyeRenderContribution out) {
        out.irisTargetX = 0f;
        out.irisTargetY = 0f;
        out.irisWeight = Curves.lerp(i.prevWeight, i.weight, pt);
    }

    @Override
    public void tick(BehaviorInstance i) {
        i.prevWeight = i.weight;
        // Ease in over the first 25%, hold centered, ease back out over the last 25%.
        i.weight = Curves.trapezoid((float) i.age / i.duration, 0.25f, 0.25f);
    }
}
