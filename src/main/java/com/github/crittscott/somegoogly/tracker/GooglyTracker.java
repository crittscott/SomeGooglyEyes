package com.github.crittscott.somegoogly.tracker;

import com.github.crittscott.somegoogly.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.head.HeadInfo;
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

    // --- Keystone B: the one behaviour this mob is currently playing (or null when idle) ----------
    // Behaviours are scheduled server-side, one at a time and non-interruptable; the client just plays
    // the active instance, advancing it each tick and interpolating it at render time. All of its state
    // is transient and client-only (no NBT, no sync of progress — only the trigger is sent).
    @Nullable
    public BehaviorInstance active;

    public static class EyeInfo {
        public float prevRotationYaw;
        public float rotationYaw;
        public float prevRotationPitch;
        public float rotationPitch;
        public float prevRotationRoll;
        public float rotationRoll;

        public float prevDeltaX;
        public float prevDeltaY;
        public float deltaX;
        public float deltaY;
        public float momentumX;
        public float momentumY;

        public EyeInfo() {
            prevDeltaY = deltaY = -1F;
        }

        /**
         * One tick of googly physics. Decoupled from the tracker so it can be reused for a held eye
         * item (see {@code GooglyEyeItemRenderer}) — the behaviour must be identical to mob eyes.
         *
         * @param rand      randomness source (per-tracker / per-held-eye)
         * @param headYaw   the holder's head yaw this tick ({@code getYHeadRot})
         * @param headPitch the holder's pitch this tick ({@code getXRot})
         * @param motionX/Y/Z the holder's position delta this tick
         */
        public void update(Random rand, float headYaw, float headPitch, double motionX, double motionY, double motionZ) {
            prevRotationYaw = rotationYaw;
            prevRotationPitch = rotationPitch;
            prevRotationRoll = rotationRoll;

            rotationYaw = headYaw;
            rotationPitch = headPitch;
            rotationRoll = 0.0f; // Most entities don't roll

            prevDeltaX = deltaX;
            prevDeltaY = deltaY;

            float yawDiff = rotationYaw - prevRotationYaw;
            float pitchDiff = rotationPitch - prevRotationPitch;
            float rollDiff = rotationRoll - prevRotationRoll;

            momentumY += motionY * 1.5F + (motionX + motionZ) * rand.nextGaussian() * (0.75F) + (pitchDiff / 45F) + (yawDiff / 180F) + rollDiff * rand.nextGaussian() * (0.05F);
            momentumX -= (motionX + motionZ) * rand.nextGaussian() * 0.4F + (yawDiff / 45F) + rollDiff * rand.nextGaussian() * (0.05F);

            float momentumLoss = 0.9F;
            float newDeltaX = deltaX + momentumX;
            float newDeltaY = deltaY + momentumY;
            if (newDeltaX < -1F || newDeltaX > 1F) {
                float newMo = momentumX * -momentumLoss;
                float randFloat = 0.8F + rand.nextFloat() * 0.2F;
                momentumX = newMo * randFloat;
                momentumY += newMo * (randFloat) * (rand.nextFloat() > 0.5F ? 1F : -1F);
            }
            if (newDeltaY < -1F || newDeltaY > 1F) {
                float newMo = momentumY * -momentumLoss;
                float randFloat = 0.8F + rand.nextFloat() * 0.2F;
                momentumY = newMo * randFloat;
                momentumX += newMo * (1F - randFloat) * (rand.nextFloat() > 0.5F ? 1F : -1F);
            } else {
                momentumY -= Mth.clamp(1F + deltaY, 0F, 0.1999F);
            }

            momentumX *= 0.95F;
            deltaX *= 0.95F;

            if (Math.abs(momentumX) < 0.03F) {
                momentumX = 0F;
            }
            if (Math.abs(deltaX) < 0.03F) {
                deltaX = 0F;
            }

            float maxMomentum = 1.3F;
            momentumX = Mth.clamp(momentumX, -maxMomentum, maxMomentum);
            momentumY = Mth.clamp(momentumY, -maxMomentum, maxMomentum);

            deltaX += momentumX;
            deltaY += momentumY;
            deltaX = Mth.clamp(deltaX, -1F, 1F);
            deltaY = Mth.clamp(deltaY, -1F, 1F);
        }
    }

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

        // Advance the active behaviour (if any), retiring it when it runs out. Done before the physics
        // so a behaviour and the wobble share the same tick's prev/current snapshot.
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

    /**
     * Start a behaviour now, unless one is already playing — the "one at a time, non-interruptable"
     * rule. Returns whether it started (a dropped trigger returns {@code false}). Called from the
     * trigger packet on the client.
     *
     * <p>{@code elapsed} fast-forwards the behaviour by that many ticks before it's shown, so a player
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

    /** Whether a behaviour is currently playing (used to drop overlapping triggers client-side). */
    public boolean isBusy() {
        return active != null;
    }

    public void setLastUpdateRequest() {
        lastUpdateRequest = ClientEventHandler.clientTicks;
    }

    public void requireUpdate() {
        shouldUpdate = true;
    }

    public boolean matches(HeadInfo helper) {
        // Compare the selected variant's head list, not just the shared config: two mobs of the same
        // type/age can resolve to different arrangements and must not share a tracker (the eyes[][]
        // shape is sized from this variant's heads/eyes).
        return this.helper.headsRef() == helper.headsRef();
    }
}
