package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class GooglyTracker {
    public final LivingEntity parent;
    public final HeadInfo helper;
    public final Random rand;

    public boolean shouldUpdate = true;
    public int lastUpdateRequest;

    public double motionX;
    public double motionY;
    public double motionZ;

    private double prevX, prevY, prevZ;

    public EyeInfo[][] eyes;

    // --- Keystone B: the one behavior this mob is currently playing (or null when idle) ----------
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
        public float prevRotationYaw;
        public float rotationYaw;
        public float prevRotationPitch;
        public float rotationPitch;

        // Pupil position in [-1,1], living in a unit disk (|p| <= 1) mapped onto the full cornea circle.
        public float prevDeltaX;
        public float prevDeltaY;
        public float deltaX;
        public float deltaY;
        // Pupil velocity.
        public float momentumX;
        public float momentumY;

        // Input differentiation state, primed on the first tick to avoid a spike from zero baselines.
        private boolean primed;
        private float prevYawRate;
        private float prevPitchRate;
        private double prevMotionX;
        private double prevMotionY;
        private double prevMotionZ;

        public EyeInfo() {
            prevDeltaY = deltaY = -1F;
        }

        /**
         * One tick of googly physics: a point-mass pupil in the eye's local plane (+X right, +Y up),
         * constrained to the unit disk (the full cornea circle once mapped by {@code moveIris}).
         * Semi-implicit Euler: accumulate forces -> integrate velocity -> integrate position -> resolve
         * the circular wall.
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
         * @param rand      randomness source (per-tracker / per-held-eye)
         * @param headYaw   the holder's head yaw this tick ({@code getYHeadRot})
         * @param headPitch the holder's pitch this tick ({@code getXRot})
         * @param motionX/Y/Z the holder's position delta this tick
         */
        public void update(Random rand, float headYaw, float headPitch, double motionX, double motionY, double motionZ) {
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

            // Pseudo-forces oppose the socket's acceleration; gravity is constant local-down.
            float fx = -R_K_YAW * yawAccel - R_K_LIN * horizRight + (float) rand.nextGaussian() * R_NOISE;
            float fy = -R_GRAVITY - R_K_PITCH * pitchAccel - R_K_VERT * (float) laccy
                    + (float) rand.nextGaussian() * R_NOISE;
            fx = Mth.clamp(fx, -R_FORCE_CLAMP, R_FORCE_CLAMP);
            fy = Mth.clamp(fy, -R_FORCE_CLAMP, R_FORCE_CLAMP);

            momentumX = (momentumX + fx) * R_AIR;
            momentumY = (momentumY + fy) * R_AIR;

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

    public void requireUpdate() {
        shouldUpdate = true;
    }

    public void setLastUpdateRequest() {
        lastUpdateRequest = ClientEventHandler.clientTicks;
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
        for (int t = 0; t < elapsed && instance.age < instance.duration; t++) {
            instance.age++;
            behavior.tick(instance);
        }
        if (instance.age >= instance.duration) {
            return false; // already finished by the time we'd show it (stale catch-up); nothing to play
        }
        active = instance;
        return true;
    }

    public void update() {
        if (!shouldUpdate) {
            return;
        }
        shouldUpdate = false;

        motionX = parent.getX() - prevX;
        motionY = parent.getY() - prevY;
        motionZ = parent.getZ() - prevZ;

        prevX = parent.getX();
        prevY = parent.getY();
        prevZ = parent.getZ();

        // Advance the active behavior (if any), retiring it when it runs out. Done before the physics
        // so a behavior and the wobble share the same tick's prev/current snapshot.
        if (active != null) {
            active.age++;
            active.behavior.tick(active);
            if (active.age >= active.duration) {
                active = null;
            }
        }

        for (int i = 0; i < eyes.length; i++) {
            for (int i1 = 0; i1 < eyes[i].length; i1++) {
                eyes[i][i1].update(rand, parent.getYHeadRot(), parent.getXRot(), motionX, motionY, motionZ);
            }
        }
    }
}
