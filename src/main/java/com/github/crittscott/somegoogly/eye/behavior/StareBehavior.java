package com.github.crittscott.somegoogly.eye.behavior;

/**
 * Pupils are sprung to center and held, then released so they drop back into the physics wobble. Drives
 * the pupil via a center spring whose stiffness eases in, holds, and eases back out; when it fades,
 * gravity carries the pupil down naturally (no snap).
 */
final class StareBehavior extends AbstractEyeBehavior {

    private static final float STIFFNESS = 1.5f;

    StareBehavior() {
        super("stare", 50);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        float t = (float) i.age / i.duration;
        out.anchorX = 0f;
        out.anchorY = 0f;
        // Ease in over the first 25%, hold centered, ease back out over the last 25%.
        out.stiffness = STIFFNESS * Curves.trapezoid(t, 0.25f, 0.25f);
    }
}
