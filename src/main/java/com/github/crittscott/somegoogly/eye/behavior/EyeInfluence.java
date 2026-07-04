package com.github.crittscott.somegoogly.eye.behavior;

/**
 * What a single active {@link EyeBehavior} exerts on one eye for one <b>simulation tick</b> (not one
 * frame). This is how behaviors participate in the physics model instead of overwriting it in the
 * renderer: the pupil is a point-mass, and a behavior that wants to move it contributes a <b>spring</b>
 * — an anchor point in the eye's unit disk plus a stiffness — that the simulator adds to the ordinary
 * forces (gravity, socket pseudo-forces). When the stiffness fades at the end of the effect, gravity
 * naturally takes the pupil back down; there is no blend and no separate hidden simulation to snap to.
 *
 * <ul>
 *   <li><b>anchorX/anchorY + stiffness</b> — the pupil spring. {@code stiffness == 0} means the behavior
 *       doesn't drive the pupil at all (it keeps wobbling freely): grow, blink, and color leave it there.
 *       Stare anchors at center, side/cross anchor off to one side, swirl's anchor orbits.</li>
 *   <li><b>eyeScaleMul</b> — multiplies the eye's configured scale (grow). Non-physical overlay.</li>
 *   <li><b>squashY</b> — multiplies the eye's vertical scale (blink). Non-physical overlay.</li>
 *   <li><b>corneaTint / tintAmount</b> — lerp the cornea toward {@code corneaTint} by {@code tintAmount}
 *       (color-change). Non-physical overlay.</li>
 * </ul>
 *
 * <p>Reused per eye: the simulator calls {@link #reset()} before asking the active behavior to fill it,
 * so an eye with no active behavior keeps the neutral defaults below (pure physics, no overlays).
 */
public final class EyeInfluence {

    /** Pupil spring anchor in the eye's unit disk (only meaningful when {@link #stiffness} &gt; 0). */
    public float anchorX;
    public float anchorY;
    /** Spring stiffness pulling the pupil toward the anchor; 0 = the behavior doesn't touch the pupil. */
    public float stiffness;

    /** Multiplies the configured eye scale (1 = unchanged). */
    public float eyeScaleMul;

    /** Multiplies the eye's vertical scale (1 = unchanged, &lt;1 = squashed for a blink). */
    public float squashY;

    /** Target cornea color to blend toward, or {@code null} for no color change. */
    public float[] corneaTint;

    /** How far to blend the cornea toward {@link #corneaTint} (0..1). */
    public float tintAmount;

    public EyeInfluence() {
        reset();
    }

    /** Restore the neutral "no behavior" state. */
    public void reset() {
        anchorX = 0f;
        anchorY = 0f;
        stiffness = 0f;
        eyeScaleMul = 1f;
        squashY = 1f;
        corneaTint = null;
        tintAmount = 0f;
    }
}
