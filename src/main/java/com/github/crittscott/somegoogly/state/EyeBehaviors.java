package com.github.crittscott.somegoogly.state;

/**
 * DESIGN NOTE / FUTURE SEAM — not yet implemented.
 *
 * <p>Today the eye "behaviours" (the Keystone B expressions: stare, blink/wink, hurt-grow, anger
 * tint, villager swirl) are hardcoded in {@code GooglyTracker}. That's fine while there are a handful
 * driven by built-in entity state. It won't scale to the intended "long list of events, some needing
 * post-event processing to match our own criteria, attached to arbitrary mobs".
 *
 * <p>When that day comes, the intended shape is a registry rather than more {@code if (mob instanceof
 * ...)} branches:
 *
 * <ul>
 *   <li><b>{@code Registry<ResourceLocation, EyeBehavior>}</b> — each behaviour is Java code
 *       registered under an id. Adding a behaviour = add a class + register it; no schema change.</li>
 *   <li><b>{@code EyeBehavior}</b> interface with hooks:
 *     <ul>
 *       <li>{@code clientTick(EyeContext)} — for behaviours derived by polling synced entity state
 *           each tick (how anger/swirl/hurt already work in {@code GooglyTracker}); the context would
 *           expose the entity, tracker, and per-eye animation handles.</li>
 *       <li>{@code onServerEvent(...)} — for behaviours fired by Forge events, including the case the
 *           user flagged: catch a broad event, then run custom matching/post-processing before
 *           deciding whether (and how) the behaviour triggers.</li>
 *     </ul></li>
 *   <li><b>{@link EyeProperties} gains {@code effects: List<EffectRef>}</b> — an eye carries which
 *       behaviours it has (id + params). Because {@code EyeProperties} is codec-driven with optional
 *       fields, adding this list is non-breaking across item NBT, mob override, and the sync packet.</li>
 * </ul>
 *
 * <p>First migration targets when this is built: the existing Keystone B effects in
 * {@code GooglyTracker} — port each into a registered {@code EyeBehavior}.
 *
 * <p>Intentionally has no code: building the registry now would be speculative. This class exists so
 * the plan is recorded where the next implementer will find it.
 */
public final class EyeBehaviors {
    private EyeBehaviors() {
    }
}
