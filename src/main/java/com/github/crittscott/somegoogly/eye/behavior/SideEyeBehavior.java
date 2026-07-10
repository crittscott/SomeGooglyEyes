package com.github.crittscott.somegoogly.eye.behavior;

/**
 * Pupils are sprung to center, then the anchor slides slowly to one side (chosen from the seed) and
 * holds there before the spring releases. Drives the pupil: the stiffness eases in during the brief
 * centring and out at the very end, so the held side-glance drops back to physics naturally.
 */
final class SideEyeBehavior extends AbstractEyeBehavior {

    private static final float AMOUNT = 0.85f;
    private static final float CENTER_FRAC = 0.1f; // fraction spent centring before the slide
    private static final float HOLD_FRAC = 0.1f;   // fraction held at the side at the end
    private static final float STIFFNESS = 1.5f;

    SideEyeBehavior() {
        super("side_eye", 90);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        float t = (float) i.age / i.duration;
        out.anchorX = i.dirSign * AMOUNT * Curves.slide(t, CENTER_FRAC, HOLD_FRAC);
        out.anchorY = 0f;
        out.stiffness = STIFFNESS * Curves.trapezoid(t, CENTER_FRAC, HOLD_FRAC);
    }

    @Override
    public void onStart(BehaviorInstance i) {
        i.dirSign = i.rand.nextBoolean() ? 1 : -1;
    }
}
