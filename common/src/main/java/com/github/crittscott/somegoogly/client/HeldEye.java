package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import javax.annotation.Nullable;

/**
 * The live eye a player holds: one {@link GooglyTracker.EyeInfo} pupil, stepped with the same physics as
 * mob eyes from the holder's look and position deltas, plus the behaviors it plays. Only the holder sees
 * it, in first person, so its behaviors start on the holder's own client: now and then it winks, stares, or
 * glances aside, from the server's ambient pool; it may bulge when the holder is hurt ({@link LivingEntity#hurtTime},
 * set by vanilla's damage event), at the server's {@code growOnHitPercent}; and it swirls when the holder heals,
 * under the server's {@code swirlOnHeal} and its cooldown. All of these come from the synced
 * {@link BehaviorSettings}. One behavior plays at a time, as on a mob. Cross-eye, which needs a partner eye, and
 * the ongoing behaviors don't apply.
 *
 * <p>It steps only while the holder holds a Googly Eye, and starts at rest, with nothing playing, each time
 * one is picked up or the holder changes.
 */
public final class HeldEye {

    /** The behavior playing, or {@code null}. */
    @Nullable
    public BehaviorInstance active;
    private int ambientCountdown;
    private List<EyeBehavior> ambientPool = List.of();
    @Nullable
    private BehaviorSettings ambientSettings;
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

    private void begin(LivingEntity holder, BehaviorSettings settings) {
        this.holder = holder;
        eye = new GooglyTracker.EyeInfo();
        active = null;
        ambientCountdown = settings.nextAmbientTicks(rand);
        healCooldown = 0;
        prevHealth = holder.getHealth();
        prevHurt = holder.hurtTime > 0;
        prevX = holder.getX();
        prevY = holder.getY();
        prevZ = holder.getZ();
    }

    /** Whether {@code entity} holds a Googly Eye in either hand. */
    public static boolean holdsGooglyEye(LivingEntity entity) {
        return entity.getMainHandItem().is(ModContent.GOOGLY_EYE.get())
                || entity.getOffhandItem().is(ModContent.GOOGLY_EYE.get());
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
     * Start one of the server's ambient expressions, if ambient play is on; the pool's cross-eye is skipped,
     * since a lone eye has no partner to cross toward.
     */
    private void startAmbient(BehaviorSettings settings) {
        if (!settings.ambient()) {
            return;
        }
        if (ambientSettings != settings) {
            ambientSettings = settings;
            ambientPool = settings.pool().stream().filter(behavior -> behavior != EyeBehavior.CROSS_EYE).toList();
        }
        if (!ambientPool.isEmpty()) {
            start(ambientPool.get(rand.nextInt(ambientPool.size())));
        }
    }

    /**
     * Advance one tick for {@code holder}, or reset when there is none or it holds no Googly Eye: age the
     * active behavior, start any behavior this tick's hurt, heal, or idle timer calls for under
     * {@code settings}, and step the pupil with the behavior folded in.
     */
    public void tick(@Nullable LivingEntity holder, BehaviorSettings settings) {
        if (holder == null || !holdsGooglyEye(holder)) {
            reset();
            return;
        }
        if (eye == null || holder != this.holder) {
            begin(holder, settings);
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
            if (rand.nextInt(ServerConfig.PERCENT_MAX) < settings.growOnHitPercent()) {
                start(EyeBehavior.GROW);
            }
        } else if (health > prevHealth && settings.swirlOnHeal() && healCooldown == 0
                && start(EyeBehavior.SWIRL)) {
            // Armed only on an actual start, so a heal while busy doesn't cost the next one its swirl.
            healCooldown = settings.swirlHealCooldownTicks();
        }
        prevHurt = hurt;
        prevHealth = health;
        if (--ambientCountdown <= 0) {
            startAmbient(settings);
            ambientCountdown = settings.nextAmbientTicks(rand);
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
