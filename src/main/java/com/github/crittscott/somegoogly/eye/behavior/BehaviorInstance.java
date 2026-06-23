package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.HeadInfo;

import java.util.Random;

/**
 * The mutable runtime state of the one behavior a mob is currently playing. Behaviors themselves are
 * stateless singletons ({@link EyeBehaviors}); all per-play state lives here, on the mob's client
 * tracker, so it's transient and per-mob.
 *
 * <p>Animated scalars are kept as {@code prev}/current pairs and advanced once per client tick in
 * {@link EyeBehavior#tick}, then interpolated with {@code partialTicks} in {@link EyeBehavior#contribute}
 * — the same smoothing idiom the eye physics uses. The fields are a small shared palette the seven
 * behaviors draw from (each uses only what it needs); randomness is seeded so every client watching the
 * same mob animates identically.
 */
public final class BehaviorInstance {

    public final EyeBehavior behavior;
    public final HeadInfo helper;
    public final int duration;
    public final Random rand;

    /** Completed ticks (1-based after the first tick); {@code age/duration} is the progress fraction. */
    public int age;

    // --- shared interpolatable channels (prev = last tick, current = this tick) ---
    public float prevWeight, weight;     // iris blend weight (0 = physics, 1 = target)
    public float prevX, x;               // iris target x (uniform behaviors)
    public float prevY, y;               // iris target y
    public float prevScale, scale;       // eye scale multiplier
    public float prevSquash, squash;     // vertical squash multiplier
    public float prevTint, tint;         // color blend amount

    // --- params resolved once in onStart (from the seed) ---
    public float[] tintColor;            // color-change target
    public int dirSign;                  // side-eye direction (+1 / -1)
    public boolean[][] mask;             // blink: which [head][eye] participate

    public BehaviorInstance(EyeBehavior behavior, HeadInfo helper, int duration, long seed) {
        this.behavior = behavior;
        this.helper = helper;
        this.duration = Math.max(1, duration);
        this.rand = new Random(seed);
        this.scale = prevScale = 1f;
        this.squash = prevSquash = 1f;
    }
}
