package com.github.crittscott.somegoogly.eye.behavior;

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
 */
public final class EyeBehaviors {

    // The event-driven behaviors are exposed as constants so their in-code drivers (the scheduler's
    // hit/trade/heal reactions) reference them directly; id lookup is only for names from the wire.
    public static final EyeBehavior GROW = new GrowBehavior();
    public static final EyeBehavior SWIRL = new SwirlBehavior();

    private static final List<EyeBehavior> ALL = List.of(
            new StareBehavior(),
            new BlinkBehavior(),
            GROW,
            new ColorChangeBehavior(),
            SWIRL,
            new SideEyeBehavior(),
            new CrossEyeBehavior()
    );
    private static final Map<ResourceLocation, EyeBehavior> BY_ID = new LinkedHashMap<>();

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
