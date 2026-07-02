package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.world.phys.Vec3;

/**
 * Pupils are sprung to center, then their anchors slide toward each eye's configured cross-target — the
 * other eye it should look at ({@link EyePlacement#crossTarget()}, an index within the same head) — and
 * hold before the spring releases. Each eye aims the direction to its partner, projected into its own
 * pupil-plane, so convergence is correct regardless of how many eyes a head has or how they're aimed.
 * An eye with no configured target (the default) simply doesn't cross.
 */
final class CrossEyeBehavior extends AbstractEyeBehavior {

    private static final float AMOUNT = 0.8f;
    private static final float CENTER_FRAC = 0.1f;
    private static final float HOLD_FRAC = 0.1f;
    private static final float STIFFNESS = 1.5f;

    CrossEyeBehavior() {
        super("cross_eye", 70);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        EyePlacement self = i.helper.placementAt(head, eye);
        int targetIdx = self.crossTarget();
        // No partner, self-reference, or a stale/out-of-range index → this eye stays neutral.
        if (targetIdx < 0 || targetIdx == eye || targetIdx >= i.helper.getEyeCount(head)) {
            return;
        }

        // Direction from this eye to its target, in the head frame, projected into this eye's pupil plane.
        Vec3 d = i.helper.placementAt(head, targetIdx).position().subtract(self.position());
        float[] dir = HeadInfo.projectToPupilPlane(self.inclination(), self.azimuth(), d.x, d.y, d.z);
        float len = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);

        float t = (float) i.age / i.duration;
        float mag = AMOUNT * slide(t);
        // Negate: ModelGooglyEye#moveIris renders the pupil at -(normX, normY) (render space is -Y up /
        // -X right), so the pupil coordinate that visually points toward the target is the negated
        // projection. Without this the pupil rolls away from its partner instead of toward it.
        if (len > 1e-4f) {
            out.anchorX = -dir[0] / len * mag;
            out.anchorY = -dir[1] / len * mag;
        }
        out.stiffness = STIFFNESS * Curves.trapezoid(t, CENTER_FRAC, HOLD_FRAC);
    }

    private static float slide(float t) {
        if (t <= CENTER_FRAC) {
            return 0f;
        }
        float moveSpan = 1f - CENTER_FRAC - HOLD_FRAC;
        return Curves.ease((t - CENTER_FRAC) / moveSpan);
    }
}
