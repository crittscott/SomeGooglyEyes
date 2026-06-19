package com.github.crittscott.somegoogly.tracker;

import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
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

    // --- Keystone B: transient, client-only expression state (no NBT, no sync) ---------------
    // All of this is derived from the entity's live state each client tick and interpolated at
    // render time with the same prev/current + partialTicks idiom as the wobble physics below.

    private static final int BLINK_DURATION = 6;     // ticks for a full close→open
    private static final int BLINK_MIN = 40;         // min ticks between blinks (~2s)
    private static final int BLINK_MAX = 200;        // max ticks between blinks (~10s)
    private static final float WINK_CHANCE = 0.15F;  // chance a blink is a single-eye wink

    private static final int STARE_COOLDOWN_MIN = 100;
    private static final int STARE_COOLDOWN_MAX = 400;
    private static final int STARE_DUR_MIN = 20;
    private static final int STARE_DUR_MAX = 60;

    private static final int SWIRL_DURATION = 30;    // villager level-up swirl length
    private static final float SWIRL_SPEED = 0.6F;   // radians/tick

    // Blink/wink scheduler. winkHead < 0 means "both eyes blink".
    public int blinkCooldown;
    public int blinkTicks;
    public int winkHead = -1;
    public int winkEye = -1;

    // Stare scheduler (random idle bouts).
    public int stareCooldown;
    public int stareTicks;
    public float prevStareAmount, stareAmount;

    // Hurt-grow (driven by hurtTime) and anger tint (driven by client-visible aggression).
    public float prevHurt, hurt;
    public float prevAnger, anger;
    // Debug pin: when true, anger is held at full regardless of the mob's real aggression.
    public boolean angerOverride;

    // Villager level-up swirl.
    public int lastVillagerLevel;
    public int swirlTicks;
    public float prevSwirlAngle, swirlAngle;

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

        // Keystone B: per-eye blink (0 = open … 1 = fully closed); per-eye so a wink closes one.
        public float prevBlink;
        public float blink;

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

            prevBlink = blink;

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

        this.blinkCooldown = BLINK_MIN + rand.nextInt(BLINK_MAX - BLINK_MIN);
        this.stareCooldown = STARE_COOLDOWN_MIN + rand.nextInt(STARE_COOLDOWN_MAX - STARE_COOLDOWN_MIN);
        this.lastVillagerLevel = parent instanceof Villager v ? v.getVillagerData().getLevel() : 0;

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

        float blinkValue = updateExpressions();

        for (int i = 0; i < eyes.length; i++) {
            for (int i1 = 0; i1 < eyes[i].length; i1++) {
                EyeInfo eye = eyes[i][i1];
                eye.update(rand, parent.getYHeadRot(), parent.getXRot(), motionX, motionY, motionZ);
                boolean affected = winkHead < 0 || (i == winkHead && i1 == winkEye);
                eye.blink = (blinkTicks > 0 && affected) ? blinkValue : 0F;
            }
        }
    }

    /** Advance the tracker-level expression timers/blends; returns this tick's blink amount (0..1). */
    private float updateExpressions() {
        prevStareAmount = stareAmount;
        prevHurt = hurt;
        prevAnger = anger;
        prevSwirlAngle = swirlAngle;

        // Hurt-grow: hurtTime ramps to 10 on hit then counts down — read it straight.
        hurt = Mth.clamp(parent.hurtTime / 10F, 0F, 1F);

        // Anger: ease toward the (best-effort, client-visible) aggression state, or a debug pin.
        float angerTarget = (angerOverride || isAngryClientSide(parent)) ? 1F : 0F;
        anger += (angerTarget - anger) * 0.1F;

        // Stare: random idle bouts, eased so pupils glide to centre and back.
        float stareTarget;
        if (stareTicks > 0) {
            stareTicks--;
            stareTarget = 1F;
        } else {
            stareCooldown--;
            if (stareCooldown <= 0) {
                stareTicks = STARE_DUR_MIN + rand.nextInt(STARE_DUR_MAX - STARE_DUR_MIN);
                stareCooldown = STARE_COOLDOWN_MIN + rand.nextInt(STARE_COOLDOWN_MAX - STARE_COOLDOWN_MIN);
            }
            stareTarget = 0F;
        }
        stareAmount += (stareTarget - stareAmount) * 0.2F;

        // Swirl: triggered when a villager's (synced) level increases.
        if (parent instanceof Villager v) {
            int level = v.getVillagerData().getLevel();
            if (level > lastVillagerLevel) {
                swirlTicks = SWIRL_DURATION;
            }
            lastVillagerLevel = level;
        }
        if (swirlTicks > 0) {
            swirlTicks--;
            swirlAngle += SWIRL_SPEED;
        }

        // Blink/wink scheduler.
        if (blinkTicks > 0) {
            blinkTicks--;
        } else {
            blinkCooldown--;
            if (blinkCooldown <= 0) {
                blinkTicks = BLINK_DURATION;
                blinkCooldown = BLINK_MIN + rand.nextInt(BLINK_MAX - BLINK_MIN);
                if (rand.nextFloat() < WINK_CHANCE) {
                    winkHead = rand.nextInt(eyes.length);
                    winkEye = eyes[winkHead].length > 0 ? rand.nextInt(eyes[winkHead].length) : -1;
                    if (winkEye < 0) {
                        winkHead = -1; // chosen head had no eyes; fall back to blinking all
                    }
                } else {
                    winkHead = -1;
                }
            }
        }
        // 0 → 1 → 0 over the blink window.
        return blinkTicks > 0 ? (float) Math.sin(Math.PI * (1.0 - blinkTicks / (double) BLINK_DURATION)) : 0F;
    }

    /** True for swirl-active villagers this frame (render uses the interpolated {@link #swirlAngle}). */
    public boolean isSwirling() {
        return swirlTicks > 0;
    }

    // --- Debug triggers (driven by the /sg debug client command) -----------------------------

    /** Start a blink now; {@code wink} closes a single random eye instead of all. */
    public void forceBlink(boolean wink) {
        blinkTicks = BLINK_DURATION;
        if (wink && eyes.length > 0) {
            winkHead = rand.nextInt(eyes.length);
            winkEye = eyes[winkHead].length > 0 ? rand.nextInt(eyes[winkHead].length) : -1;
            if (winkEye < 0) {
                winkHead = -1;
            }
        } else {
            winkHead = -1;
        }
    }

    /** Stare (pupils centred) for at least {@code ticks} more ticks. */
    public void forceStare(int ticks) {
        stareTicks = Math.max(stareTicks, ticks);
    }

    /** Start a pupil swirl now (works on any mob, not just levelling villagers). */
    public void forceSwirl() {
        swirlTicks = SWIRL_DURATION;
    }

    /** Pin anger on/off, overriding the mob's real aggression state. */
    public void forceAnger(boolean on) {
        angerOverride = on;
    }

    /** Best-effort, client-visible aggression test. Mobs whose anger isn't synced won't tint. */
    private static boolean isAngryClientSide(LivingEntity entity) {
        if (entity instanceof EnderMan enderMan) {
            return enderMan.isCreepy();
        }
        if (entity instanceof Wolf wolf) {
            return wolf.isAngry();
        }
        if (entity instanceof Mob mob) {
            return mob.isAggressive();
        }
        return false;
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
