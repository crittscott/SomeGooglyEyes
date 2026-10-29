package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * The live eye a player holds: one {@link GooglyTracker.EyeInfo} pupil, stepped with the same physics as
 * mob eyes from the holder's look and position deltas, plus the behaviors it plays. Only the holder sees
 * it, in first person, so its behaviors start on the holder's own client: now and then it winks, stares, or
 * glances aside; it bulges when the holder is hurt ({@link LivingEntity#hurtTime}, set by vanilla's damage
 * event) and swirls when the holder heals. One behavior plays at a time, as on a mob. Cross-eye, which
 * needs a partner eye, and the ongoing mob states (squints, lids, anger, pulse, rattle) don't apply.
 *
 * <p>It steps only while the holder holds a Googly Eye, and starts at rest, with nothing playing, each time
 * one is picked up or the holder changes.
 */
public final class HeldEye {

    /** Longest and shortest idle wait between behaviors, in ticks; the server's ambient defaults. */
    public static final int AMBIENT_MAX_TICKS = 800;
    public static final int AMBIENT_MIN_TICKS = 200;
    /** Pool the idle timer picks from. */
    private static final EyeBehavior[] AMBIENT_POOL = {EyeBehavior.BLINK, EyeBehavior.SIDE_EYE, EyeBehavior.STARE};
    /** Ticks after a heal swirl before healing can swirl again; the server's heal-swirl default. */
    public static final int HEAL_COOLDOWN_TICKS = 200;

    /** The behavior playing, or {@code null}. */
    @Nullable
    public BehaviorInstance active;
    private int ambientCountdown;
    /** The pupil, or {@code null} while no Googly Eye is held. */
    @Nullable
    public GooglyTracker.EyeInfo eye;
    private int healCooldown;
    @Nullable
    private LivingEntity holder;
    // Reused each tick; the held eye is stepped only on the client tick thread.
    private final EyeInfluence influence = new EyeInfluence();
    private float prevHealth;
    private boolean prevHurt;
    private double prevX, prevY, prevZ;
    private final RandomSource rand;

    public HeldEye() {
        this(RandomSource.create());
    }

    public HeldEye(RandomSource rand) {
        this.rand = rand;
    }

    /** Whether {@code entity} holds a Googly Eye in either hand. */
    public static boolean holdsGooglyEye(LivingEntity entity) {
        return entity.getMainHandItem().is(ModContent.GOOGLY_EYE.get())
                || entity.getOffhandItem().is(ModContent.GOOGLY_EYE.get());
    }

    private void begin(LivingEntity holder) {
        this.holder = holder;
        eye = new GooglyTracker.EyeInfo();
        active = null;
        ambientCountdown = nextAmbientDelay();
        healCooldown = 0;
        prevHealth = holder.getHealth();
        prevHurt = holder.hurtTime > 0;
        prevX = holder.getX();
        prevY = holder.getY();
        prevZ = holder.getZ();
    }

    private int nextAmbientDelay() {
        return AMBIENT_MIN_TICKS + rand.nextInt(AMBIENT_MAX_TICKS - AMBIENT_MIN_TICKS + 1);
    }

    /** Drop the eye and anything playing on it. */
    public void reset() {
        holder = null;
        eye = null;
        active = null;
    }

    /**
     * Start {@code behavior} at its default length, unless one is already playing or no eye is held.
     * Returns whether it started.
     */
    public boolean start(EyeBehavior behavior) {
        if (eye == null || active != null) {
            return false;
        }
        BehaviorInstance instance = new BehaviorInstance(behavior, null, behavior.defaultDuration(), rand.nextLong());
        behavior.onStart(instance);
        active = instance;
        return true;
    }

    /**
     * Advance one tick for {@code holder}, or reset when there is none or it holds no Googly Eye: age the
     * active behavior, start any behavior this tick's hurt, heal, or idle timer calls for, and step the
     * pupil with the behavior folded in.
     */
    public void tick(@Nullable LivingEntity holder) {
        if (holder == null || !holdsGooglyEye(holder)) {
            reset();
            return;
        }
        if (eye == null || holder != this.holder) {
            begin(holder);
        }

        // Retire a finished behavior first, so this tick's triggers can start the next one.
        if (active != null) {
            active.age++;
            if (active.age >= active.duration) {
                active = null;
            }
        }
        if (healCooldown > 0) {
            healCooldown--;
        }

        boolean hurt = holder.hurtTime > 0;
        float health = holder.getHealth();
        if (hurt && !prevHurt) {
            start(EyeBehavior.GROW);
        } else if (health > prevHealth && healCooldown == 0 && start(EyeBehavior.SWIRL)) {
            // Armed only on an actual start, so a heal while busy doesn't cost the next one its swirl.
            healCooldown = HEAL_COOLDOWN_TICKS;
        }
        prevHurt = hurt;
        prevHealth = health;
        if (--ambientCountdown <= 0) {
            start(AMBIENT_POOL[rand.nextInt(AMBIENT_POOL.length)]);
            ambientCountdown = nextAmbientDelay();
        }

        double mx = holder.getX() - prevX;
        double my = holder.getY() - prevY;
        double mz = holder.getZ() - prevZ;
        prevX = holder.getX();
        prevY = holder.getY();
        prevZ = holder.getZ();

        influence.reset();
        if (active != null) {
            active.behavior.influence(active, 0, 0, influence);
        }
        eye.applyOverlays(influence, 1F, 1F, 0F, 0F);
        eye.update(rand, holder.getYHeadRot(), holder.getXRot(), mx, my, mz,
                influence.anchorX, influence.anchorY, influence.stiffness, 0F);
    }
}
