package com.github.crittscott.somegoogly.eye.behavior;

/** Eyes briefly bulge larger and settle back. Non-physical overlay: drives the eye-scale channel. */
final class GrowBehavior extends AbstractEyeBehavior {

    private static final float GROW = 0.5f; // extra scale at the peak

    GrowBehavior() {
        super("grow", 14);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        out.eyeScaleMul = 1f + GROW * Curves.sinPulse((float) i.age / i.duration);
    }
}
