package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.eye.behavior.OngoingBehavior;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-entity client-side eye state: the {@link EyeInfo} pupil-physics simulator for every configured
 * eye, plus which {@link BehaviorInstance} (if any) is currently playing on that entity.
 *
 * <p>One tracker is kept per tracked {@link LivingEntity} (see {@link ClientEyeRuntime}), matched
 * against the entity's current placement via {@link #matches}. {@link #update} advances the active
 * behavior and steps every eye's physics once per client tick; {@link #startBehavior} applies a
 * server-triggered behavior, enforcing the "one at a time, non-interruptable" rule.
 *
 * <p>Each tick also folds in the {@link OngoingBehavior}s whose server switches are on, easing each one in or
 * out over a few ticks: the sleeping squint, or a sleeping villager's lids closing to a slit; the ramming
 * goat's squint; the archer's one-eyed aim, closing the eye away from the arm holding the weapon; the swelling
 * creeper's rattle, kicking its pupils back into flight in proportion to the swell; red pupils for an attacking
 * mob or one the server reports {@link #angry}; and the creepy enderman's pupil pulse, at a frequency of its
 * own. None of these blocks a behavior: each sits on top of whatever plays. A sleeping entity starts no new
 * behavior whether or not its squint is on.
 */
public class GooglyTracker {
    // Reused each tick on the single client tick thread (one tracker updated at a time), so folding the
    // active behavior into the simulation allocates nothing per tick.
    private static final EyeInfluence INFLUENCE = new EyeInfluence();

    /** Eye height multiplier while fully squinting (asleep or ramming). */
    public static final float SLEEP_SQUASH_Y = 0.3F;
    /** Ticks to ease fully into or out of the squint. */
    public static final int SLEEP_EASE_TICKS = 10;
    /**
     * How much of the eye each of a sleeping villager's lids covers when fully closed, leaving a slit
     * between them across the middle.
     */
    public static final float SLEEP_LID_COVERAGE = 0.45F;
    /** Ticks for a villager's lids to slide fully down or back up. */
    public static final int SLEEP_LID_EASE_TICKS = 20;
    /** Sleeping villager eyelid color. */
    public static final float[] SLEEP_LID_COLOR = {0.82F, 0.68F, 0.52F};
    /** Off-hand eye height multiplier while fully aiming. */
    public static final float AIM_SQUASH_Y = 0.15F;
    /** Ticks to ease fully into or out of the aiming squint; a skeleton draws its bow for about 20. */
    public static final int AIM_EASE_TICKS = 4;
    /** Pupil color while fully angry. */
    public static final float[] ANGER_IRIS_COLOR = {0.9F, 0.05F, 0.05F};
    /** Ticks to ease fully into or out of angry red pupils. */
    public static final int ANGER_EASE_TICKS = 10;
    /** Slowest and fastest creepy-enderman pupil pulse, in cycles per second. */
    public static final float CREEPY_MIN_HZ = 0.5F;
    public static final float CREEPY_MAX_HZ = 2F;
    /** Range of each eye's offset into the creepy pulse, in radians. */
    public static final float CREEPY_MIN_PHASE_OFFSET = Mth.HALF_PI;
    public static final float CREEPY_MAX_PHASE_OFFSET = 3F * Mth.HALF_PI;
    /** How far a creepy enderman's pupils swell and shrink around their size (fraction of it). */
    public static final float CREEPY_PULSE_AMPLITUDE = 0.35F;
    /** Ticks to ease fully into or out of the creepy pulse. */
    public static final int CREEPY_EASE_TICKS = 10;

    /** The placement this tracker's eyes were built for. */
    public final HeadInfo helper;
    /** The entity whose eyes this tracker simulates. */
    public final LivingEntity parent;
    /** Per-entity randomness for pupil jitter, seeded from the entity's UUID. */
    public final RandomSource rand;

    /**
     * The client tick during which this tracker was last rendered. {@link ClientEyeRuntime} evicts the
     * tracker once this goes stale and simulates it only while it is being rendered, so off-screen eyes
     * freeze instead of wobbling unseen.
     */
    public int lastRenderTick;

    /** The entity's position change over the last {@link #update}. */
    public double motionX, motionY, motionZ;

    private double prevX, prevY, prevZ;

    /** Pupil simulators, indexed by head then eye, shaped by this tracker's placement. */
    public EyeInfo[][] eyes;

    /**
     * The entity's current appearance override, mirrored from {@code EyeStatePacket} so render layers
     * don't re-read it from NBT every frame. Seeded from NBT at construction and kept current by
     * {@link ClientNetworkHandler#handleEyeState}.
     */
    public AppearanceOverride overrides;

    /**
     * The behavior playing on this entity, or {@code null}. Advanced each tick and interpolated at render;
     * client-only, since the server sends only the trigger.
     */
    @Nullable
    public BehaviorInstance active;

    /**
     * Whether the server reports this entity angry in a way vanilla doesn't sync (see {@code
     * EyeAngerPacket}); kept current by {@link ClientNetworkHandler#handleAnger}.
     */
    public boolean angry;

    /** How far into the squint (sleeping or ramming) the eyes are: 0 open, 1 fully squinted. */
    private float squint;
    /** How far a sleeping villager's lids are closed: 0 out of sight, 1 down to the slit. */
    private float lid;
    /** How far the off-hand eye is squinted for aiming: 0 open, 1 fully squinted. */
    private float aim;
    /**
     * Per head, the index of the eye farthest to the entity's left and to its right, or -1 for both when
     * the head has no eye off to one side. Eye positions are in head model space, where +X is the entity's
     * right.
     */
    private final int[] leftEye;
    private final int[] rightEye;
    /** How red the pupils are: 0 calm, 1 fully angry. */
    private float anger;
    /** How strongly the creepy pulse plays: 0 not creepy, 1 full pulse. */
    private float creepiness;
    /** Creepy pulse phase in radians, and how far it advances each tick. */
    private float creepyPhase;
    private final float creepyStep;

    /**
     * Build the tracker. {@code angry} is the server-reported anger already known for this entity, so a
     * tracker built for an angry mob starts red, as one built for a sleeping mob starts squinted or lidded;
     * {@code settings} says which ongoing behaviors are on.
     */
    public GooglyTracker(@Nonnull LivingEntity parent, @Nonnull HeadInfo helper, boolean angry,
                         @Nonnull BehaviorSettings settings) {
        this.parent = parent;
        this.helper = helper;
        this.rand = RandomSource.create(parent.getUUID().getLeastSignificantBits());
        // Drawn from the UUID-seeded source, so every viewer sees the same enderman pulse at the same rate,
        // and each eye at the same offset within it.
        float hz = Mth.lerp(rand.nextFloat(), CREEPY_MIN_HZ, CREEPY_MAX_HZ);
        this.creepyStep = Mth.TWO_PI * hz / SharedConstants.TICKS_PER_SECOND;
        this.angry = angry;
        this.overrides = EyeState.readProperties(parent);
        this.eyes = new EyeInfo[helper.getHeadCount()][];
        this.leftEye = new int[eyes.length];
        this.rightEye = new int[eyes.length];

        for (int i = 0; i < eyes.length; i++) {
            this.eyes[i] = new EyeInfo[helper.getEyeCount(i)];
            int left = 0;
            int right = 0;
            for (int i1 = 0; i1 < this.eyes[i].length; i1++) {
                this.eyes[i][i1] = new EyeInfo();
                this.eyes[i][i1].creepyPhaseOffset =
                        Mth.lerp(rand.nextFloat(), CREEPY_MIN_PHASE_OFFSET, CREEPY_MAX_PHASE_OFFSET);
                double x = helper.placementAt(i, i1).position().x;
                if (x < helper.placementAt(i, left).position().x) {
                    left = i1;
                }
                if (x > helper.placementAt(i, right).position().x) {
                    right = i1;
                }
            }
            // A lone or centered eye stays open: closing it would read as a blink, not aiming.
            boolean sided = left != right;
            this.leftEye[i] = sided ? left : -1;
            this.rightEye[i] = sided ? right : -1;
        }

        this.prevX = parent.getX();
        this.prevY = parent.getY();
        this.prevZ = parent.getZ();
        // A tracker built for an entity already in an ongoing state starts in it rather than easing in.
        this.squint = isSquinting(settings) ? 1F : 0F;
        this.lid = isLidded(settings) ? 1F : 0F;
        this.aim = level(OngoingBehavior.AIM_WINK, settings);
        this.anger = isAngry(settings) ? 1F : 0F;
        this.creepiness = level(OngoingBehavior.CREEPY_PULSE, settings);
        update(settings);
        for (EyeInfo[] head : eyes) {
            for (EyeInfo info : head) {
                info.prevSquashY = info.squashY;
                info.prevLid = info.lid;
                info.prevIrisTintAmount = info.irisTintAmount;
            }
        }
    }

    /**
     * Physics state for one pupil: its position and velocity in the eye's unit disk, the head
     * orientation used to derive angular forcing, and the non-physical overlay values (grow/blink/tint)
     * an active behavior writes each tick. {@link #update} steps all of it by one tick.
     */
    public static class EyeInfo {
        // --- tuning (normalized units; boundary radius = 1) ---------------------------------------
        private static final float R_AIR = 1.0f;               // free-flight velocity damping per tick (1 = none)
        private static final float R_FORCE_CLAMP = 0.96f;      // cap any single tick's forcing (jump spikes)
        private static final float R_GRAVITY = 0.03f;          // constant downward pull (units/tick^2)
        private static final float R_K_LIN = 12.0f;            // mob horizontal accel -> sideways kick (per blk/tick^2)
        private static final float R_K_PITCH = 0.012f;         // head pitch accel -> vertical kick
        private static final float R_K_VERT = 4.0f;            // mob vertical accel -> vertical kick
        private static final float R_K_YAW = 0.012f;           // head yaw accel -> sideways kick (per deg/tick^2)
        private static final float R_NOISE = 0.0005f;           // tiny per-eye jitter so eyes don't lock-step
        // R_REST_CUTOFF must stay above the resting rebound (R_RESTITUTION * R_GRAVITY) or the pupil
        // jitters on the rim forever; below it and it parks. With 0.8*0.03=0.024, 0.028 is safe.
        private static final float R_REST_CUTOFF = 0.028f;     // kill rebounds slower than this (stop bouncing)
        private static final float R_RESTITUTION = 0.8f;       // bounce energy kept (0..1); higher = livelier
        private static final float R_SLIDE_CUTOFF = 0.004f;    // park a slow pupil sitting on the rim
        private static final float R_TANGENT_FRICTION = 0.03f; // tangential energy lost per wall contact
        private static final float R_AGITATED_MIN_SPEED = 1.5f;  // speed floor at full agitation (units/tick)
        private static final float R_AGITATED_KICK = 1.6f;       // random-direction kick at full agitation when below the floor

        /** Head orientation, kept across ticks to differentiate into angular velocity and acceleration. */
        public float prevRotationPitch;
        /** See {@link #prevRotationPitch}. */
        public float prevRotationYaw;
        /** See {@link #prevRotationPitch}. */
        public float rotationPitch;
        /** See {@link #prevRotationPitch}. */
        public float rotationYaw;

        /** Pupil position in the unit disk ({@code |p| <= 1}), mapped onto the full cornea circle. */
        public float deltaX;
        /** See {@link #deltaX}. */
        public float deltaY;
        /** Pupil position at the previous tick, for render interpolation. */
        public float prevDeltaX;
        /** See {@link #prevDeltaX}. */
        public float prevDeltaY;
        /** Pupil velocity per tick. */
        public float momentumX;
        /** See {@link #momentumX}. */
        public float momentumY;

        /**
         * World-down projected into this eye's pupil plane, in the ({@code deltaX}, {@code deltaY})
         * convention. Refreshed each render by {@code GooglyEyeRenderer} from the eye's animated world
         * orientation, so the pupil sags toward true down; {@code (0, -1)}, eye-local down, until the
         * first render.
         */
        public float gravX = 0F;
        /** See {@link #gravX}. */
        public float gravY = -1F;

        /**
         * Non-physical behavior overlays, written each tick by the active behavior (neutral when none)
         * and interpolated at render. Kept per eye so the blink mask varies by eye. This one is the
         * grow scale; {@link #irisScaleMul} is the love pupil size, {@link #squashY} the blink,
         * {@link #tintAmount} the color-change blend, and {@link #tintColor} the color-change target (set
         * instantly, not interpolated).
         */
        public float scaleMul = 1F, prevScaleMul = 1F;
        /** Love pupil size times the creepy pulse; see {@link #scaleMul}. */
        public float irisScaleMul = 1F, prevIrisScaleMul = 1F;
        /** Blink squash times the sleeping squint; see {@link #scaleMul}. */
        public float squashY = 1F, prevSquashY = 1F;
        /** How much of the eye each of a sleeping villager's lids covers (0 none); see {@link #scaleMul}. */
        public float lid, prevLid;
        /** Color-change blend amount; see {@link #scaleMul}. */
        public float tintAmount, prevTintAmount;
        /** Color-change target color; see {@link #scaleMul}. */
        public float[] tintColor;
        /** How far the pupil is blended toward {@link #ANGER_IRIS_COLOR}; see {@link #scaleMul}. */
        public float irisTintAmount, prevIrisTintAmount;
        /** This eye's offset into the creepy pulse, in radians, so an enderman's eyes don't throb in step. */
        public float creepyPhaseOffset;

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
         * Fold this tick's non-physical overlays into the eye's render state, keeping last tick's values for
         * lerping: the active behavior's {@code influence} (neutral when none), with the ongoing states the
         * caller derives on top of it.
         *
         * @param influence  the behavior's influence on this eye this tick
         * @param irisPulse  pupil size multiplier from the creepy pulse (1 for none)
         * @param squash     vertical scale multiplier from the squints (1 for none)
         * @param lid        how much of the eye each lid covers (0 for none)
         * @param irisTint   how far the pupil is blended toward {@link #ANGER_IRIS_COLOR} (0 for none)
         */
        public void applyOverlays(EyeInfluence influence, float irisPulse, float squash, float lid, float irisTint) {
            prevScaleMul = scaleMul;
            scaleMul = influence.eyeScaleMul;
            prevIrisScaleMul = irisScaleMul;
            irisScaleMul = influence.irisScaleMul * irisPulse;
            prevSquashY = squashY;
            squashY = influence.squashY * squash;
            prevLid = this.lid;
            this.lid = lid;
            prevTintAmount = tintAmount;
            tintAmount = influence.tintAmount;
            tintColor = influence.corneaTint;
            prevIrisTintAmount = irisTintAmount;
            irisTintAmount = irisTint;
        }

        /**
         * Advance the pupil simulation by one tick and update its previous/current render positions.
         * Pupil positions and behavior anchors use the eye-local unit disk, with +X right and +Y up.
         *
         * @param rand      randomness source (per-tracker / per-held-eye)
         * @param headYaw   the holder's head yaw this tick ({@code getYHeadRot})
         * @param headPitch the holder's pitch this tick ({@code getXRot})
         * @param motionX   the holder's X position delta this tick
         * @param motionY   the holder's Y position delta this tick
         * @param motionZ   the holder's Z position delta this tick
         * @param anchorX   the behavior spring's X target in the unit disk (ignored when stiffness is nonpositive)
         * @param anchorY   the behavior spring's Y target in the unit disk (ignored when stiffness is nonpositive)
         * @param stiffness positive behavior-spring strength; nonpositive values disable the spring
         * @param agitation 0 to 1; above 0, a pupil slower than its scaled speed floor gets a random kick
         */
        public void update(RandomSource rand, float headYaw, float headPitch, double motionX, double motionY, double motionZ,
                           float anchorX, float anchorY, float stiffness, float agitation) {
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

            // Agitation feeds energy back in: a pupil that has slowed below the floor is kicked in a random
            // direction, so gravity and the wall bounce keep shaping its flight instead of letting it park.
            float minSpeed = agitation * R_AGITATED_MIN_SPEED;
            if (minSpeed > 0F && momentumX * momentumX + momentumY * momentumY < minSpeed * minSpeed) {
                float angle = rand.nextFloat() * Mth.TWO_PI;
                momentumX += agitation * R_AGITATED_KICK * Mth.cos(angle);
                momentumY += agitation * R_AGITATED_KICK * Mth.sin(angle);
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

    /** Whether this tracker was built for the same placement variant as {@code helper}. */
    public boolean matches(HeadInfo helper) {
        // Compare the selected variant's head list, not just the shared config: two mobs of the same
        // type/age can resolve to different arrangements and must not share a tracker (the eyes[][]
        // shape is sized from this variant's heads/eyes).
        return this.helper.headsRef() == helper.headsRef();
    }

    /**
     * Stamp this tracker as rendered during {@code currentClientTick}, the
     * {@link ClientEyeRuntime#clientTicks()} value (drives eviction and whether to simulate).
     */
    public void markRendered(int currentClientTick) {
        lastRenderTick = currentClientTick;
    }

    /**
     * Start a behavior now, unless one is already playing — the "one at a time, non-interruptable"
     * rule — or the entity is asleep. Returns whether it started (a dropped trigger returns
     * {@code false}). Called from the trigger packet on the client.
     *
     * <p>{@code elapsed} starts the behavior that many ticks in, so a player who starts watching a mob
     * mid-effect sees the same state as everyone else: the seeded {@code onStart} plus the age fully
     * determine each behavior's influence.
     */
    public boolean startBehavior(@Nonnull EyeBehavior behavior, int duration, long seed, int elapsed) {
        if (active != null || parent.isSleeping()) {
            return false;
        }
        BehaviorInstance instance = new BehaviorInstance(behavior, helper, duration, seed);
        behavior.onStart(instance);
        // Fast-forward to where the effect already is (mid-join catch-up). Each behavior derives its
        // influence from age alone, so advancing age is all the catch-up needs — no per-tick replay.
        instance.age = elapsed;
        if (instance.age >= instance.duration) {
            return false; // already finished by the time we'd show it (stale catch-up); nothing to play
        }
        active = instance;
        return true;
    }

    /**
     * Advance one client tick: sample the entity's motion, age the active behavior, and step every eye, with
     * the ongoing behaviors {@code settings} switches on folded in.
     */
    public void update(BehaviorSettings settings) {
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

        // Ease toward the squint or back out. A behavior playing under it (one already playing when sleep
        // begins, or any while a goat rams) multiplies with it in squashY.
        squint = ease(squint, isSquinting(settings), SLEEP_EASE_TICKS);
        float squintSquash = Mth.lerp(squint, 1F, SLEEP_SQUASH_Y);
        // A sleeping villager lowers its lids instead, leaving its eyes their full shape behind them.
        lid = ease(lid, isLidded(settings), SLEEP_LID_EASE_TICKS);
        float lidCoverage = lid * SLEEP_LID_COVERAGE;

        // Aiming closes only the eye opposite the arm holding the weapon, sighting along it with the other,
        // and multiplies with the squint and any behavior the same way.
        aim = ease(aim, level(OngoingBehavior.AIM_WINK, settings) > 0F, AIM_EASE_TICKS);
        float aimSquash = Mth.lerp(aim, 1F, AIM_SQUASH_Y);
        HumanoidArm weaponArm = parent.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? parent.getMainArm() : parent.getMainArm().getOpposite();
        int[] aimClosedEye = weaponArm == HumanoidArm.RIGHT ? leftEye : rightEye;

        // Anger reddens the pupils, and a creepy enderman's pupils pulse; both sit under any behavior.
        anger = ease(anger, isAngry(settings), ANGER_EASE_TICKS);
        creepiness = ease(creepiness, level(OngoingBehavior.CREEPY_PULSE, settings) > 0F, CREEPY_EASE_TICKS);
        creepyPhase = (creepyPhase + creepyStep) % Mth.TWO_PI;
        float creepyAmplitude = CREEPY_PULSE_AMPLITUDE * creepiness;

        // A swelling creeper's pupils rattle harder the closer it is to exploding.
        float agitation = level(OngoingBehavior.SWELL_RATTLE, settings);

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

                info.applyOverlays(INFLUENCE,
                        1F + creepyAmplitude * Mth.sin(creepyPhase + info.creepyPhaseOffset),
                        squintSquash * (i1 == aimClosedEye[i] ? aimSquash : 1F), lidCoverage, anger);

                // Step the pupil physics, with the behavior's spring folded in as a force.
                info.update(rand, headYaw, headPitch, motionX, motionY, motionZ,
                        INFLUENCE.anchorX, INFLUENCE.anchorY, INFLUENCE.stiffness, agitation);
            }
        }
    }

    /**
     * Whether the pupils should be red: anger is on, and the mob is attacking (vanilla-synced) or the server
     * says it's angry.
     */
    private boolean isAngry(BehaviorSettings settings) {
        return settings.enabled(OngoingBehavior.ANGER) && (angry || OngoingBehavior.ANGER.level(parent) > 0F);
    }

    /** Whether the eyelids should be closed: the sleeping squint is on and the entity is a sleeping villager. */
    private boolean isLidded(BehaviorSettings settings) {
        return parent instanceof Villager && level(OngoingBehavior.SLEEP_SQUINT, settings) > 0F;
    }

    /**
     * Whether the eyes should squint: the entity sleeps without lids of its own, or is a goat with its head
     * lowered to ram, each while its switch is on.
     */
    private boolean isSquinting(BehaviorSettings settings) {
        return !(parent instanceof Villager) && level(OngoingBehavior.SLEEP_SQUINT, settings) > 0F
                || level(OngoingBehavior.RAM_SQUINT, settings) > 0F;
    }

    /** How strongly {@code behavior} applies to this entity now, or 0 while its switch is off. */
    private float level(OngoingBehavior behavior, BehaviorSettings settings) {
        return settings.enabled(behavior) ? behavior.level(parent) : 0F;
    }

    /** Step {@code value} one tick toward 1 when {@code on}, else toward 0, taking {@code ticks} to cross. */
    private static float ease(float value, boolean on, int ticks) {
        float step = 1F / ticks;
        return on ? Math.min(1F, value + step) : Math.max(0F, value - step);
    }
}
