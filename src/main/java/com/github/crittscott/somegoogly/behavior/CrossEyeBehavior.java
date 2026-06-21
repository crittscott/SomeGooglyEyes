package com.github.crittscott.somegoogly.behavior;

import com.github.crittscott.somegoogly.head.HeadInfo;

/**
 * Pupils centre, then roll inward toward each other (cross-eyed) and hold. Each eye's "inward" is the
 * sign of its configured horizontal offset, so the left and right eyes converge regardless of layout.
 * Drives the iris channel; the magnitude eases out in {@code x} while {@code weight} ramps to 1.
 *
 * <p>The inward sign is derived from the eye's config x-offset and may need flipping per how a given
 * model's local axes fall — tunable via {@link #INWARD_SIGN} after seeing it in-game.
 */
final class CrossEyeBehavior extends AbstractEyeBehavior {

    private static final float AMOUNT = 0.8f;
    private static final float CENTER_FRAC = 0.1f;
    private static final float HOLD_FRAC = 0.1f;
    /** Flip if eyes diverge instead of converging on a given model. */
    private static final float INWARD_SIGN = -1f;

    CrossEyeBehavior() {
        super("cross_eye", 70);
    }

    @Override
    public void tick(BehaviorInstance i) {
        i.prevWeight = i.weight;
        i.prevX = i.x;
        float t = (float) i.age / i.duration;
        i.weight = Curves.ease(t / CENTER_FRAC);
        i.x = AMOUNT * slide(t); // magnitude; per-eye direction applied in contribute
    }

    private static float slide(float t) {
        if (t <= CENTER_FRAC) {
            return 0f;
        }
        float moveSpan = 1f - CENTER_FRAC - HOLD_FRAC;
        return Curves.ease((t - CENTER_FRAC) / moveSpan);
    }

    @Override
    public void contribute(BehaviorInstance i, HeadInfo helper, int head, int eye, float pt, EyeRenderContribution out) {
        float offsetX = helper.getEyeOffsetFromJoint(head, eye)[0];
        // Centred/ambiguous eyes (offset ~0) fall back to alternating by index so they still cross.
        float dir = offsetX != 0f ? Math.signum(offsetX) : (eye % 2 == 0 ? 1f : -1f);
        float magnitude = Curves.lerp(i.prevX, i.x, pt);
        out.irisTargetX = INWARD_SIGN * dir * magnitude;
        out.irisTargetY = 0f;
        out.irisWeight = Curves.lerp(i.prevWeight, i.weight, pt);
    }
}
