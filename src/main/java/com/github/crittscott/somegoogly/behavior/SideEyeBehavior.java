package com.github.crittscott.somegoogly.behavior;

import com.github.crittscott.somegoogly.head.HeadInfo;

/**
 * Pupils snap to center, then slide slowly to one side (chosen from the seed) and hold there. Drives the
 * iris channel: {@code irisWeight} ramps to 1 during the brief centring, then {@code x} eases out to the
 * side over the bulk of the duration.
 */
final class SideEyeBehavior extends AbstractEyeBehavior {

    private static final float AMOUNT = 0.85f;
    private static final float CENTER_FRAC = 0.1f; // fraction spent centring before the slide
    private static final float HOLD_FRAC = 0.1f;   // fraction held at the side at the end

    SideEyeBehavior() {
        super("side_eye", 90);
    }

    @Override
    public void onStart(BehaviorInstance i) {
        i.dirSign = i.rand.nextBoolean() ? 1 : -1;
    }

    @Override
    public void tick(BehaviorInstance i) {
        i.prevWeight = i.weight;
        i.prevX = i.x;
        float t = (float) i.age / i.duration;
        i.weight = Curves.ease(t / CENTER_FRAC); // ease() clamps to 1
        i.x = i.dirSign * AMOUNT * slide(t);
    }

    /** 0 while centring, eases 0 → 1 across the middle, holds 1 at the end. */
    private static float slide(float t) {
        if (t <= CENTER_FRAC) {
            return 0f;
        }
        float moveSpan = 1f - CENTER_FRAC - HOLD_FRAC;
        return Curves.ease((t - CENTER_FRAC) / moveSpan);
    }

    @Override
    public void contribute(BehaviorInstance i, HeadInfo helper, int head, int eye, float pt, EyeRenderContribution out) {
        out.irisTargetX = Curves.lerp(i.prevX, i.x, pt);
        out.irisTargetY = 0f;
        out.irisWeight = Curves.lerp(i.prevWeight, i.weight, pt);
    }
}
