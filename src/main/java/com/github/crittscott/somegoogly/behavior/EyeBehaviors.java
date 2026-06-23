package com.github.crittscott.somegoogly.behavior;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The registry of eye behaviors — the seven expressions a mob can play, keyed by id. A plain map, not a
 * Forge registry: these are code-defined client animations, and both sides only need the ids to agree
 * (the server names a behavior in the trigger packet; the client looks it up to play). This class is
 * deliberately free of client-only references so the server can class-load it to pick and schedule.
 *
 * <p>This replaces the old {@code state.EyeBehaviors} design note: the "registry rather than more
 * {@code instanceof} branches" plan, now realised.
 */
public final class EyeBehaviors {

    private static final Map<ResourceLocation, EyeBehavior> BY_ID = new LinkedHashMap<>();
    private static final List<EyeBehavior> ALL = List.of(
            new StareBehavior(),
            new BlinkBehavior(),
            new GrowBehavior(),
            new ColorChangeBehavior(),
            new SwirlBehavior(),
            new SideEyeBehavior(),
            new CrossEyeBehavior()
    );

    static {
        for (EyeBehavior behavior : ALL) {
            BY_ID.put(behavior.id(), behavior);
        }
    }

    private EyeBehaviors() {
    }

    /** All registered behaviors, in a stable order. */
    public static List<EyeBehavior> all() {
        return ALL;
    }

    /** Look up a behavior by id, or {@code null} if unknown (e.g. an id from a newer/older build). */
    @Nullable
    public static EyeBehavior byId(ResourceLocation id) {
        return BY_ID.get(id);
    }
}
