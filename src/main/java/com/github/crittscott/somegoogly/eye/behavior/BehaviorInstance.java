package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.HeadInfo;

import java.util.Random;

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

    public final EyeBehavior behavior;
    public final HeadInfo helper;
    public final int duration;
    public final Random rand;

    /** Completed ticks (1-based after the first tick); {@code age/duration} is the progress fraction. */
    public int age;

    // --- params resolved once in onStart (from the seed) ---
    public float[] tintColor;            // color-change target
    public int dirSign;                  // side-eye direction (+1 / -1)
    public boolean[][] mask;             // blink: which [head][eye] participate

    public BehaviorInstance(EyeBehavior behavior, HeadInfo helper, int duration, long seed) {
        this.behavior = behavior;
        this.helper = helper;
        this.duration = Math.max(1, duration);
        this.rand = new Random(seed);
    }
}
