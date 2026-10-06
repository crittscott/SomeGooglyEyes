package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The seven named eye expressions a mob can play, keyed by a {@link ResourceLocation} id. These are
 * code-defined client animations, and both sides only need the ids to agree: the server names a
 * behavior by id in the trigger packet and the client looks it up to play. All per-play state lives in
 * a {@link BehaviorInstance}.
 *
 * <p>A mob plays at most one behavior at a time (the server enforces this), so behaviors never compose.
 * They participate in the eye <b>physics simulation</b>, not the renderer: once per simulation tick the
 * simulator asks the single active behavior to fill an {@link EyeInfluence} per eye — a pupil spring
 * (anchor + stiffness) and/or the non-physical overlays (scale, squash, tint). The renderer then just
 * interpolates and draws the resulting eye state; it never touches behaviors.
 *
 * <p>This type must stay free of client-only imports: the server class-loads it to pick and schedule
 * behaviors, and only the client ever calls {@link #influence}.
 */
public enum EyeBehavior {

    /**
     * Pupils are sprung to center and held, then released so they drop back into the physics wobble. A
     * center spring whose stiffness eases in, holds, and eases back out; when it fades, gravity carries
     * the pupil down naturally (no snap).
     */
    STARE("stare", 50) {
        @Override
        public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
            float t = (float) i.age / i.duration;
            out.anchorX = 0f;
            out.anchorY = 0f;
            // Ease in over the first 25%, hold centered, ease back out over the last 25%.
            out.stiffness = PUPIL_STIFFNESS * trapezoid(t, 0.25f, 0.25f);
        }
    },

    /**
     * Squashes between 1 and N of the mob's eyes shut and open again. The participating set is chosen
     * once from the seed, so a "wink" (one eye) and a full blink are the same behavior at different
     * counts. Non-physical overlay: drives the squash channel only — the pupil keeps wobbling underneath.
     */
    BLINK("blink", 8) {
        @Override
        public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
            if (i.mask != null && head < i.mask.length && eye < i.mask[head].length && i.mask[head][eye]) {
                out.squashY = 1f - BLINK_SQUASH * sinPulse((float) i.age / i.duration);
            }
        }

        @Override
        public void onStart(BehaviorInstance i) {
            int heads = i.helper.getHeadCount();
            i.mask = new boolean[heads][];
            int total = 0;
            for (int h = 0; h < heads; h++) {
                int eyes = i.helper.getEyeCount(h);
                i.mask[h] = new boolean[eyes];
                total += eyes;
            }
            if (total <= 0) {
                return;
            }
            // Pick k ∈ [1, total] eyes at random to participate (k = 1 is a wink).
            int k = 1 + i.rand.nextInt(total);
            for (int picked = 0; picked < k; ) {
                int target = i.rand.nextInt(total);
                int seen = 0;
                outer:
                for (int h = 0; h < heads; h++) {
                    for (int e = 0; e < i.mask[h].length; e++) {
                        if (seen++ == target) {
                            if (!i.mask[h][e]) {
                                i.mask[h][e] = true;
                                picked++;
                            }
                            break outer;
                        }
                    }
                }
            }
        }
    },

    /** Eyes briefly bulge larger and settle back. Non-physical overlay: drives the eye-scale channel. */
    GROW("grow", 14) {
        @Override
        public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
            out.eyeScaleMul = 1f + GROW_AMOUNT * sinPulse((float) i.age / i.duration);
        }
    },

    /**
     * Blends the cornea toward a color and back. The color is chosen from the seed (a random hue).
     * Non-physical overlay: drives the cornea-tint channel.
     */
    COLOR_CHANGE("color_change", 50) {
        @Override
        public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
            out.corneaTint = i.tintColor;
            // Ease in, hold the color, ease out.
            out.tintAmount = trapezoid((float) i.age / i.duration, 0.2f, 0.2f);
        }

        @Override
        public void onStart(BehaviorInstance i) {
            // A vivid random hue (full saturation/value) so the change reads clearly.
            int rgb = Mth.hsvToRgb(i.rand.nextFloat(), 1f, 1f);
            i.tintColor = new float[]{
                    ((rgb >> 16) & 0xFF) / 255f,
                    ((rgb >> 8) & 0xFF) / 255f,
                    (rgb & 0xFF) / 255f
            };
        }
    },

    /**
     * Pupils orbit rapidly, the orbit radius shrinking to zero so they spiral into the center as the
     * effect ends. A firm spring to a moving anchor; because the anchor itself spirals to center, the
     * pupil ends near center and the release is soft (no drop from the rim).
     */
    SWIRL("swirl", 70) {
        @Override
        public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
            float t = (float) i.age / i.duration;
            double angle = SWIRL_SPEED * i.age;
            float radius = SWIRL_RADIUS * (1f - t); // shrink to 0
            out.anchorX = (float) Math.cos(angle) * radius;
            out.anchorY = (float) Math.sin(angle) * radius;
            out.stiffness = SWIRL_STIFFNESS;
        }
    },

    /**
     * Pupils are sprung to center, then the anchor slides slowly to one side (chosen from the seed) and
     * holds there before the spring releases. The stiffness eases in during the brief centering and out
     * at the very end, so the held side-glance drops back to physics naturally.
     */
    SIDE_EYE("side_eye", 90) {
        @Override
        public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
            float t = (float) i.age / i.duration;
            out.anchorX = i.dirSign * SIDE_EYE_AMOUNT * slide(t, GLANCE_CENTER_FRAC, GLANCE_HOLD_FRAC);
            out.anchorY = 0f;
            out.stiffness = PUPIL_STIFFNESS * trapezoid(t, GLANCE_CENTER_FRAC, GLANCE_HOLD_FRAC);
        }

        @Override
        public void onStart(BehaviorInstance i) {
            i.dirSign = i.rand.nextBoolean() ? 1 : -1;
        }
    },

    /**
     * Pupils are sprung to center, then their anchors slide toward each eye's configured cross-target —
     * the other eye it should look at ({@link EyePlacement#crossTarget()}, an index within the same head)
     * — and hold before the spring releases. Each eye aims the direction to its partner, projected into
     * its own pupil plane, so convergence is correct regardless of how many eyes a head has or how
     * they're aimed. An eye with no configured target (the default) simply doesn't cross.
     */
    CROSS_EYE("cross_eye", 70) {
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
            float[] dir = self.projectToPupilPlane(d.x, d.y, d.z);
            float len = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);

            float t = (float) i.age / i.duration;
            float mag = CROSS_EYE_AMOUNT * slide(t, GLANCE_CENTER_FRAC, GLANCE_HOLD_FRAC);
            // Negate: ModelGooglyEye#moveIris renders the pupil at -(normX, normY) (render space is -Y up /
            // -X right), so the pupil coordinate that visually points toward the target is the negated
            // projection. Without this the pupil rolls away from its partner instead of toward it.
            if (len > 1e-4f) {
                out.anchorX = -dir[0] / len * mag;
                out.anchorY = -dir[1] / len * mag;
            }
            out.stiffness = PUPIL_STIFFNESS * trapezoid(t, GLANCE_CENTER_FRAC, GLANCE_HOLD_FRAC);
        }
    };

    /** Center-spring stiffness for the stare and glance behaviors. */
    private static final float PUPIL_STIFFNESS = 1.5f;
    /** Vertical squash at full close (→ ~5% height). */
    private static final float BLINK_SQUASH = 0.95f;
    /** Extra scale at the grow peak. */
    private static final float GROW_AMOUNT = 0.5f;
    private static final float SWIRL_RADIUS = 0.8f;
    /** Swirl orbit speed, radians per tick. */
    private static final float SWIRL_SPEED = 0.6f;
    /** Firm, so the pupil tracks the fast orbit. */
    private static final float SWIRL_STIFFNESS = 3.0f;
    private static final float SIDE_EYE_AMOUNT = 0.85f;
    private static final float CROSS_EYE_AMOUNT = 0.8f;
    /** Fraction of a side-eye or cross-eye spent centering before the slide. */
    private static final float GLANCE_CENTER_FRAC = 0.1f;
    /** Fraction of a side-eye or cross-eye held at the target at the end. */
    private static final float GLANCE_HOLD_FRAC = 0.1f;

    private static final Map<ResourceLocation, EyeBehavior> BY_ID = new HashMap<>();

    static {
        for (EyeBehavior behavior : values()) {
            BY_ID.put(behavior.id, behavior);
        }
    }

    private final ResourceLocation id;
    private final int defaultDuration;

    EyeBehavior(String name, int defaultDuration) {
        this.id = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, name);
        this.defaultDuration = defaultDuration;
    }

    /** Look up a behavior by id, or {@code null} for an unregistered identifier. */
    @Nullable
    public static EyeBehavior byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** The id (e.g. {@code somegoogly:stare}); used on the wire and as the config key. */
    public ResourceLocation id() {
        return id;
    }

    /** Default length in ticks when triggered without an explicit duration (ambient uses this). */
    public int defaultDuration() {
        return defaultDuration;
    }

    /**
     * Fill {@code out} (already reset) with this eye's influence for the current simulation tick, derived
     * from {@code instance.age}. Called per eye, so per-eye behaviors (blink mask, cross-eye target
     * direction) can vary their output by {@code head}/{@code eye} via {@code instance.helper}.
     */
    public abstract void influence(BehaviorInstance instance, int head, int eye, EyeInfluence out);

    /** Resolve seeded params (blink mask, color, direction) once when the effect starts. */
    public void onStart(BehaviorInstance instance) {
    }

    /** Smoothstep ease 0 → 1 over t ∈ [0,1]. */
    private static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    /** 0 → 1 → 0 over t ∈ [0,1] (a single smooth pulse). */
    private static float sinPulse(float t) {
        return (float) Math.sin(Math.PI * Mth.clamp(t, 0f, 1f));
    }

    /**
     * Slide envelope, the displacement counterpart to {@link #trapezoid}'s stiffness envelope: holds 0
     * for the first {@code centerFrac} (while the pupil is being sprung to center), eases 0 → 1 across
     * the middle, then holds 1 for the last {@code holdFrac}. Used by the behaviors that spring the
     * pupil to center and then walk the anchor somewhere and hold it (side-eye, cross-eye).
     */
    private static float slide(float t, float centerFrac, float holdFrac) {
        if (t <= centerFrac) {
            return 0f;
        }
        return ease((t - centerFrac) / (1f - centerFrac - holdFrac));
    }

    /**
     * Trapezoid envelope: eases 0 → 1 over the first {@code rise} fraction, holds 1, then eases 1 → 0
     * over the last {@code fall} fraction.
     */
    private static float trapezoid(float t, float rise, float fall) {
        t = Mth.clamp(t, 0f, 1f);
        if (t < rise) {
            return ease(t / rise);
        }
        if (t > 1f - fall) {
            return ease((1f - t) / fall);
        }
        return 1f;
    }
}
