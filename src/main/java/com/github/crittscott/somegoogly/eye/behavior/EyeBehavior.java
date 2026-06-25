package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.resources.ResourceLocation;

/**
 * One named eye expression (stare, blink, grow, …). Behaviors are stateless singletons registered in
 * {@link EyeBehaviors} under a {@link ResourceLocation} id; the server names a behavior by id in the
 * trigger packet and the client looks it up to play. All per-play state lives in a
 * {@link BehaviorInstance}.
 *
 * <p>A mob plays at most one behavior at a time (the server enforces this), so behaviors never
 * compose: each frame the renderer asks the single active behavior to fill an
 * {@link EyeRenderContribution} per eye, layered over the eye's physics-driven baseline.
 *
 * <p>Implementations must stay free of client-only imports: the server class-loads this registry to
 * pick and schedule behaviors, and only the client ever calls {@link #tick}/{@link #contribute}.
 */
public interface EyeBehavior {

    /**
     * Fill {@code out} (already reset) with this eye's contribution for the current frame, interpolating
     * the instance's scalars by {@code partialTicks}. Called per eye, so per-eye behaviors (blink mask,
     * cross-eye inward direction) can vary their output by {@code head}/{@code eye} using {@code helper}.
     */
    void contribute(BehaviorInstance instance, HeadInfo helper, int head, int eye,
                    float partialTicks, EyeRenderContribution out);

    /** Default length in ticks when triggered without an explicit duration (ambient uses this). */
    int defaultDuration();

    /** The registry id (e.g. {@code somegoogly:stare}); used on the wire and as the config key. */
    ResourceLocation id();

    /** Resolve seeded params (blink mask, color, direction) and prime the prev/current scalars. */
    default void onStart(BehaviorInstance instance) {
    }

    /** Advance one client tick: shift current scalars into prev, recompute current from {@code age}. */
    default void tick(BehaviorInstance instance) {
    }
}
