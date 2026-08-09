package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class GooglyTracker {
    // Reused each tick on the single client tick thread (one tracker updated at a time), so folding the
    // active behavior into the simulation allocates nothing per tick.
    private static final EyeInfluence INFLUENCE = new EyeInfluence();

    public final HeadInfo helper;
    public final LivingEntity parent;
    public final Random rand;

    // The client tick during which this tracker was last rendered. Drives both the tick loop's decisions
    // (see ClientEventHandler): evict once it's more than 10 ticks stale, and simulate only while it's
    // being rendered — so off-screen eyes freeze instead of wobbling on unseen.
    public int lastRenderTick;

    public double motionX, motionY, motionZ;

    private double prevX, prevY, prevZ;

    public EyeInfo[][] eyes;

        // Behaviors are scheduled server-side, one at a time and non-interruptable; the client just plays
    // the active instance, advancing it each tick and interpolating it at render time. All of its state
    // is transient and client-only (no NBT, no sync of progress — only the trigger is sent).
    @Nullable
    public BehaviorInstance active;

    public GooglyTracker(@Nonnull LivingEntity parent, @Nonnull HeadInfo helper) {
        this.parent = parent;
        this.helper = helper;
        this.rand = new Random(Math.abs(parent.getUUID().hashCode()) * 8134L);
        this.eyes = new EyeInfo[helper.getHeadCount()][];

        for (int i = 0; i < eyes.length; i++) {
            this.eyes[i] = new EyeInfo[helper.getEyeCount(i)];
            for (int i1 = 0; i1 < this.eyes[i].length; i1++) {
                this.eyes[i][i1] = new EyeInfo();
            }
        }

        this.prevX = parent.getX();
        this.prevY = parent.getY();
        this.prevZ = parent.getZ();
        update();
    }

    public static class EyeInfo {
        // --- tuning (normalized units; boundary radius = 1) ---------------------------------------
        private static final float R_AIR = 1.0f;               // free-flight velocity damping per tick (1 = none)
        private static final float R_FORCE_CLAMP = 0.96f;      // cap any single tick's forcing (jump spikes)
        private static final float R_GRAVITY = 0.03f;          // constant downward pull (units/tick^2)
        private static final float R_K_LIN = 12.0f;            // mob horizontal accel -> sideways kick (per blk/tick^2)
        private static final float R_K_PITCH = 0.012f;         // head pitch accel -> vertical kick
        private static final float R_K_VERT = 4.0f;            // mob vertical accel -> vertical kick
        private static final float R_K_YAW = 0.012f;           // head yaw accel -> sideways kick (per deg/tick^2)
        private static final float R_NOISE = 0.005f;           // tiny per-eye jitter so eyes don't lock-step
        // R_REST_CUTOFF must stay above the resting rebound (R_RESTITUTION * R_GRAVITY) or the pupil
        // jitters on the rim forever; below it and it parks. With 0.8*0.03=0.024, 0.028 is safe.
        private static final float R_REST_CUTOFF = 0.028f;     // kill rebounds slower than this (stop bouncing)
        private static final float R_RESTITUTION = 0.8f;       // bounce energy kept (0..1); higher = livelier
        private static final float R_SLIDE_CUTOFF = 0.004f;    // park a slow pupil sitting on the rim
        private static final float R_TANGENT_FRICTION = 0.03f; // tangential energy lost per wall contact

        // Head orientation, kept across ticks to differentiate into angular velocity/acceleration.
        public float prevRotationPitch;
        public float prevRotationYaw;
        public float rotationPitch;
        public float rotationYaw;

        // Pupil position in [-1,1], living in a unit disk (|p| <= 1) mapped onto the full cornea circle.
        public float deltaX;
        public float deltaY;
        public float prevDeltaX;
        public float prevDeltaY;
        // Pupil velocity.
        public float momentumX;
        public float momentumY;

        // World-down projected into this eye's pupil plane, in the (deltaX, deltaY) convention. Refreshed
        // each render (GooglyEyeRenderer) from the eye's actual animated world orientation, so the pupil
        // sags toward true down rather than eye-local down. (0, -1) — straight down — until the first
        // render, so an off-screen or not-yet-drawn eye falls toward eye-local down.
        public float gravX = 0F;
        public float gravY = -1F;

        // Non-physical behavior overlays, written each tick by the active behavior (neutral when none)
        // and interpolated by partialTicks at render. Kept per-eye so the blink mask varies by eye.
        public float scaleMul = 1F, prevScaleMul = 1F;   // grow
        public float squashY = 1F, prevSquashY = 1F;     // blink
        public float tintAmount, prevTintAmount;         // color-change blend amount
        public float[] tintColor;                        // color-change target (set instantly, not lerped)

        // Input differentiation state, primed on the first tick to avoid a spike from zero baselines.
        private double prevMotionX;
        private double prevMotionY;
        private double prevMotionZ;
        private float prevPitchRate;
        private float prevYawRate;
        private boolean primed;

        public EyeInfo() {
            prevDeltaY = deltaY = -1F;
        }

        /**
         * One tick of googly physics: a point-mass pupil in the eye's local plane (+X right, +Y up),
         * constrained to the unit disk (the full cornea circle once mapped by {@code moveIris}).
         *
         * <p>Forcing: constant local-down gravity, plus pseudo-forces from the holder's linear
         * acceleration and head angular acceleration (so the pupil lags and overshoots when the mob
         * jerks or whips its head). Walls reflect radially with restitution {@code < 1}; tangential
         * friction plus rest/slide cutoffs bleed the last energy so it parks at the bottom instead of
         * ringing forever.
         *
         * <p>Decoupled from the tracker so it can be reused for a held eye item (see
         * {@code GooglyEyeItemRenderer}) — the behavior must be identical to mob eyes.
         *
         * <p>An active behavior participates as a <b>spring</b>: it supplies an {@code anchor} and a
         * {@code stiffness}, and the pupil is pulled toward the anchor (with velocity damping) on top of
         * the ordinary forces. {@code stiffness == 0} means no behavior drives the pupil, so it wobbles
         * freely; when a fading behavior lets stiffness fall to 0, gravity takes the pupil back naturally.
         *
         * @param rand      randomness source (per-tracker / per-held-eye)
         * @param headYaw   the holder's head yaw this tick ({@code getYHeadRot})
         * @param headPitch the holder's pitch this tick ({@code getXRot})
         * @param motionX   the holder's X position delta this tick
         * @param motionY   the holder's Y position delta this tick
         * @param motionZ   the holder's Z position delta this tick
         * @param anchorX   the behavior spring's X target in the unit disk (ignored when stiffness is 0)
         * @param anchorY   the behavior spring's Y target in the unit disk (ignored when stiffness is 0)
         * @param stiffness the behavior spring's stiffness (0 = no behavior force on the pupil)
         */
        public void update(Random rand, float headYaw, float headPitch, double motionX, double motionY, double motionZ,
                           float anchorX, float anchorY, float stiffness) {
            prevDeltaX = deltaX;
            prevDeltaY = deltaY;

            // Prime the differentiators on the first tick so we don't see a spike from zero baselines.
            if (!primed) {
                primed = true;
                rotationYaw = prevRotationYaw = headYaw;
                rotationPitch = prevRotationPitch = headPitch;
                prevYawRate = 0F;
                prevPitchRate = 0F;
                prevMotionX = motionX;
                prevMotionY = motionY;
                prevMotionZ = motionZ;
                return;
            }

            prevRotationYaw = rotationYaw;
            prevRotationPitch = rotationPitch;
            rotationYaw = headYaw;
            rotationPitch = headPitch;

            // Angular acceleration (deg/tick^2); wrap yaw so the 180/-180 seam doesn't read as a spin.
            float yawRate = Mth.wrapDegrees(rotationYaw - prevRotationYaw);
            float pitchRate = rotationPitch - prevRotationPitch;
            float yawAccel = yawRate - prevYawRate;
            float pitchAccel = pitchRate - prevPitchRate;
            prevYawRate = yawRate;
            prevPitchRate = pitchRate;

            // Linear acceleration (blocks/tick^2); project the horizontal part onto the head's right axis.
            double laccx = motionX - prevMotionX;
            double laccy = motionY - prevMotionY;
            double laccz = motionZ - prevMotionZ;
            prevMotionX = motionX;
            prevMotionY = motionY;
            prevMotionZ = motionZ;

            double yawRad = Math.toRadians(rotationYaw);
            float rightX = (float) Math.cos(yawRad);
            float rightZ = (float) Math.sin(yawRad);
            float horizRight = (float) (laccx * rightX + laccz * rightZ);

            // Pseudo-forces oppose the socket's acceleration. Gravity points along this eye's copy of
            // world-down (gravX/gravY), captured each render from the eye's animated orientation, so the
            // pupil rests at true down instead of eye-local down. Its in-plane magnitude naturally shrinks
            // as the eye turns to face up/down (world-down leaves the pupil plane). The pseudo-forces stay
            // eye-local — only gravity's resting direction is a world-frame quantity. Default (0, -1) =
            // eye-local down.
            float fx = R_GRAVITY * gravX - R_K_YAW * yawAccel - R_K_LIN * horizRight
                    + (float) rand.nextGaussian() * R_NOISE;
            float fy = R_GRAVITY * gravY - R_K_PITCH * pitchAccel - R_K_VERT * (float) laccy
                    + (float) rand.nextGaussian() * R_NOISE;
            fx = Mth.clamp(fx, -R_FORCE_CLAMP, R_FORCE_CLAMP);
            fy = Mth.clamp(fy, -R_FORCE_CLAMP, R_FORCE_CLAMP);

            momentumX += fx;
            momentumY += fy;

            if (stiffness > 0F) {
                // Behavior spring (unclamped, so it can overcome gravity): Hooke pull toward the anchor,
                // plus velocity damping applied implicitly (divide, never multiply) so it stays stable at
                // any stiffness and settles without ringing. As stiffness fades the damping fades with it,
                // handing the pupil back to free-flight gravity for a natural drop.
                momentumX += stiffness * (anchorX - deltaX);
                momentumY += stiffness * (anchorY - deltaY);
                float keep = 1F / (1F + 2F * (float) Math.sqrt(stiffness));
                momentumX *= keep;
                momentumY *= keep;
            } else {
                momentumX *= R_AIR;
                momentumY *= R_AIR;
            }

            deltaX += momentumX;
            deltaY += momentumY;

            // Circular wall: reflect the outward normal component, friction the tangential, project back.
            float r2 = deltaX * deltaX + deltaY * deltaY;
            if (r2 > 1F) {
                float r = (float) Math.sqrt(r2);
                float nx = deltaX / r;
                float ny = deltaY / r;
                float vn = momentumX * nx + momentumY * ny;
                float tvx = momentumX - vn * nx;
                float tvy = momentumY - vn * ny;

                tvx *= (1F - R_TANGENT_FRICTION);
                tvy *= (1F - R_TANGENT_FRICTION);
                if (tvx * tvx + tvy * tvy < R_SLIDE_CUTOFF * R_SLIDE_CUTOFF) {
                    tvx = 0F;
                    tvy = 0F;
                }

                float rn = vn > 0F ? -R_RESTITUTION * vn : vn;
                if (Math.abs(rn) < R_REST_CUTOFF) {
                    rn = 0F;
                }

                momentumX = tvx + rn * nx;
                momentumY = tvy + rn * ny;
                deltaX = nx;
                deltaY = ny;
            }
        }
    }

    public boolean matches(HeadInfo helper) {
        // Compare the selected variant's head list, not just the shared config: two mobs of the same
        // type/age can resolve to different arrangements and must not share a tracker (the eyes[][]
        // shape is sized from this variant's heads/eyes).
        return this.helper.headsRef() == helper.headsRef();
    }

    /** Stamp this tracker as rendered this client tick (drives eviction + whether to simulate). */
    public void markRendered() {
        lastRenderTick = ClientEventHandler.clientTicks();
    }

    /**
     * Start a behavior now, unless one is already playing — the "one at a time, non-interruptable"
     * rule. Returns whether it started (a dropped trigger returns {@code false}). Called from the
     * trigger packet on the client.
     *
     * <p>{@code elapsed} fast-forwards the behavior by that many ticks before it's shown, so a player
     * who starts watching a mob mid-effect picks it up in sync with everyone else (the server sends how
     * far in the effect already is). It's deterministic — the seeded {@code onStart} plus replaying
     * {@code tick} reproduces the exact same state other viewers are at.
     */
    public boolean startBehavior(@Nonnull EyeBehavior behavior, int duration, long seed, int elapsed) {
        if (active != null) {
            return false;
        }
        BehaviorInstance instance = new BehaviorInstance(behavior, helper, duration, seed);
        behavior.onStart(instance);
        // Fast-forward to where the effect already is (mid-join catch-up). Each behavior derives its
        // influence from age alone, so advancing age is all the catch-up needs — no per-tick replay.
        instance.age = Math.max(0, Math.min(elapsed, instance.duration));
        if (instance.age >= instance.duration) {
            return false; // already finished by the time we'd show it (stale catch-up); nothing to play
        }
        active = instance;
        return true;
    }

    public void update() {
        motionX = parent.getX() - prevX;
        motionY = parent.getY() - prevY;
        motionZ = parent.getZ() - prevZ;

        prevX = parent.getX();
        prevY = parent.getY();
        prevZ = parent.getZ();

        // Advance the active behavior (if any), retiring it when it runs out. Done before the physics so
        // this tick's simulation already sees the retired (null) behavior on its final step.
        if (active != null) {
            active.age++;
            if (active.age >= active.duration) {
                active = null;
            }
        }

        float headYaw = parent.getYHeadRot();
        float headPitch = parent.getXRot();
        for (int i = 0; i < eyes.length; i++) {
            for (int i1 = 0; i1 < eyes[i].length; i1++) {
                EyeInfo info = eyes[i][i1];

                // Ask the single active behavior for this eye's influence (spring + overlays), or neutral.
                INFLUENCE.reset();
                if (active != null) {
                    active.behavior.influence(active, i, i1, INFLUENCE);
                }

                // Fold the non-physical overlays into the eye's render state (prev/current for lerping).
                info.prevScaleMul = info.scaleMul;
                info.scaleMul = INFLUENCE.eyeScaleMul;
                info.prevSquashY = info.squashY;
                info.squashY = INFLUENCE.squashY;
                info.prevTintAmount = info.tintAmount;
                info.tintAmount = INFLUENCE.tintAmount;
                info.tintColor = INFLUENCE.corneaTint;

                // Step the pupil physics, with the behavior's spring folded in as a force.
                info.update(rand, headYaw, headPitch, motionX, motionY, motionZ,
                        INFLUENCE.anchorX, INFLUENCE.anchorY, INFLUENCE.stiffness);
            }
        }
    }
}
