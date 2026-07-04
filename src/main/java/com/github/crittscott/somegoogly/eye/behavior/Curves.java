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

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** 0 → 1 → 0 over t ∈ [0,1] (a single smooth pulse). */
    static float sinPulse(float t) {
        return (float) Math.sin(Math.PI * clamp01(t));
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
