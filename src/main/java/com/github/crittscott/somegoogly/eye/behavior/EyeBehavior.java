package com.github.crittscott.somegoogly.eye.behavior;

import net.minecraft.resources.ResourceLocation;

/**
 * One named eye expression (stare, blink, grow, …). Behaviors are stateless singletons registered in
 * {@link EyeBehaviors} under a {@link ResourceLocation} id; the server names a behavior by id in the
 * trigger packet and the client looks it up to play. All per-play state lives in a
 * {@link BehaviorInstance}.
 *
 * <p>A mob plays at most one behavior at a time (the server enforces this), so behaviors never compose.
 * They participate in the eye <b>physics simulation</b>, not the renderer: once per simulation tick the
 * simulator asks the single active behavior to fill an {@link EyeInfluence} per eye — a pupil spring
 * (anchor + stiffness) and/or the non-physical overlays (scale, squash, tint). The renderer then just
 * interpolates and draws the resulting eye state; it never touches behaviors.
 *
 * <p>Implementations must stay free of client-only imports: the server class-loads this registry to
 * pick and schedule behaviors, and only the client ever calls {@link #influence}.
 */
public interface EyeBehavior {

    /**
     * Fill {@code out} (already reset) with this eye's influence for the current simulation tick, derived
     * from {@code instance.age}. Called per eye, so per-eye behaviors (blink mask, cross-eye target
     * direction) can vary their output by {@code head}/{@code eye} via {@code instance.helper}.
     */
    void influence(BehaviorInstance instance, int head, int eye, EyeInfluence out);

    /** Default length in ticks when triggered without an explicit duration (ambient uses this). */
    int defaultDuration();

    /** The registry id (e.g. {@code somegoogly:stare}); used on the wire and as the config key. */
    ResourceLocation id();

    /** Resolve seeded params (blink mask, color, direction) once when the effect starts. */
    default void onStart(BehaviorInstance instance) {
    }
}
