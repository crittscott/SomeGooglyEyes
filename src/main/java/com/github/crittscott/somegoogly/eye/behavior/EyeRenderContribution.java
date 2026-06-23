package com.github.crittscott.somegoogly.eye.behavior;

/**
 * The per-eye render delta a single active {@link EyeBehavior} contributes for one frame. The renderer
 * starts from the eye's <b>baseline</b> (physics-driven iris wobble, config eye scale, effective
 * cornea color) and folds this on top:
 *
 * <ul>
 *   <li><b>iris</b> — {@code finalIris = physics * (1 - irisWeight) + irisTarget * irisWeight}. A
 *       behavior that doesn't touch the pupil leaves {@code irisWeight = 0}, so the wobble shows
 *       through unchanged (blink, grow, color). One that drives the pupil (stare, swirl, side-eye,
 *       cross-eye) ramps the weight up and supplies a target.</li>
 *   <li><b>eyeScaleMul</b> — multiplies the eye's configured scale (grow).</li>
 *   <li><b>squashY</b> — multiplies the eye's vertical scale (blink).</li>
 *   <li><b>corneaTint</b> / <b>tintAmount</b> — lerp the effective cornea color toward
 *       {@code corneaTint} by {@code tintAmount} (color-change).</li>
 * </ul>
 *
 * <p>Reused per eye: the renderer calls {@link #reset()} before asking the active behavior to fill it,
 * so idle eyes (no active behavior) keep the neutral defaults below.
 */
public final class EyeRenderContribution {

    /** Where the behavior wants the pupil, and how strongly to pull it off the physics wobble (0..1). */
    public float irisTargetX;
    public float irisTargetY;
    public float irisWeight;

    /** Multiplies the configured eye scale (1 = unchanged). */
    public float eyeScaleMul;

    /** Multiplies the eye's vertical scale (1 = unchanged, &lt;1 = squashed for a blink). */
    public float squashY;

    /** Target cornea color to blend toward, or {@code null} for no color change. */
    public float[] corneaTint;

    /** How far to blend the cornea toward {@link #corneaTint} (0..1). */
    public float tintAmount;

    public EyeRenderContribution() {
        reset();
    }

    /** Restore the neutral "no behavior" state. */
    public void reset() {
        irisTargetX = 0f;
        irisTargetY = 0f;
        irisWeight = 0f;
        eyeScaleMul = 1f;
        squashY = 1f;
        corneaTint = null;
        tintAmount = 0f;
    }
}
