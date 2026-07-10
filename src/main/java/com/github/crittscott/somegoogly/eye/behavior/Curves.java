package com.github.crittscott.somegoogly.eye.behavior;

/** Small easing/envelope helpers shared by the behaviors. */
final class Curves {

    private Curves() {
    }

    static float clamp01(float t) {
        return t < 0f ? 0f : Math.min(t, 1f);
    }

    /** Smoothstep ease 0 → 1 over t ∈ [0,1]. */
    static float ease(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }

    /** 0 → 1 → 0 over t ∈ [0,1] (a single smooth pulse). */
    static float sinPulse(float t) {
        return (float) Math.sin(Math.PI * clamp01(t));
    }

    /**
     * Slide envelope, the displacement counterpart to {@link #trapezoid}'s stiffness envelope: holds 0
     * for the first {@code centerFrac} (while the pupil is being sprung to center), eases 0 → 1 across
     * the middle, then holds 1 for the last {@code holdFrac}. Shared by the behaviors that spring the
     * pupil to center and then walk the anchor somewhere and hold it (side-eye, cross-eye).
     */
    static float slide(float t, float centerFrac, float holdFrac) {
        if (t <= centerFrac) {
            return 0f;
        }
        return ease((t - centerFrac) / (1f - centerFrac - holdFrac));
    }

    /**
     * Trapezoid envelope: eases 0 → 1 over the first {@code rise} fraction, holds 1, then eases 1 → 0
     * over the last {@code fall} fraction.
     */
    static float trapezoid(float t, float rise, float fall) {
        t = clamp01(t);
        if (t < rise) {
            return ease(t / rise);
        }
        if (t > 1f - fall) {
            return ease((1f - t) / fall);
        }
        return 1f;
    }
}
