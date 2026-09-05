package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

/**
 * The mutable runtime state of the one behavior a mob is currently playing. Behaviors themselves are
 * stateless singletons ({@link EyeBehaviors}); all per-play state lives here, on the mob's client
 * tracker, so it's transient and per-mob.
 *
 * <p>The only animated state is {@link #age}: each behavior derives its {@link EyeInfluence} directly
 * from {@code age/duration} in {@link EyeBehavior#influence}, so there are no prev/current scalar pairs
 * to keep — the smoothing lives one level down, in the pupil's own physics interpolation and in the
 * per-eye overlay state the simulator tracks. The seeded params below are resolved once in
 * {@link EyeBehavior#onStart}; randomness is seeded so every client watching the same mob animates
 * identically.
 */
public final class BehaviorInstance {

    /** Stateless behavior definition being played. */
    public final EyeBehavior behavior;
    /** Placement view whose head and eye indexes define per-eye behavior state. */
    public final HeadInfo helper;
    /** Total play length in ticks, normalized to at least one. */
    public final int duration;
    /** Deterministic random source initialized from the server-supplied seed. */
    public final RandomSource rand;

    /** Completed ticks (1-based after the first tick); {@code age/duration} is the progress fraction. */
    public int age;

    /** Color-change target, or {@code null} until a color-change behavior initializes it. */
    @Nullable
    public float[] tintColor;
    /** Seeded side-eye direction: {@code -1} or {@code +1} after side-eye initialization. */
    public int dirSign;
    /** Seeded blink participation indexed by {@code [head][eye]}, or {@code null} for other behaviors. */
    @Nullable
    public boolean[][] mask;

    /** Create unstarted per-play state; the caller next invokes {@link EyeBehavior#onStart}. */
    public BehaviorInstance(EyeBehavior behavior, HeadInfo helper, int duration, long seed) {
        this.behavior = behavior;
        this.helper = helper;
        this.duration = Math.max(1, duration);
        this.rand = RandomSource.create(seed);
    }
}
