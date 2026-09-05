package com.github.crittscott.somegoogly.eye.behavior;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The registry of eye behaviors — the seven expressions a mob can play, keyed by id. These are
 * code-defined client animations, and both sides only need the ids to agree (the server names a
 * behavior in the trigger packet; the client looks it up to play). This class is deliberately free of
 * client-only references so the server can class-load it to pick and schedule.
 */
public final class EyeBehaviors {

    // Built-ins referenced by configuration defaults or event drivers are constants; id lookup is
    // reserved for configured or network-provided names.
    public static final EyeBehavior BLINK = new BlinkBehavior();
    public static final EyeBehavior CROSS_EYE = new CrossEyeBehavior();
    public static final EyeBehavior GROW = new GrowBehavior();
    public static final EyeBehavior SIDE_EYE = new SideEyeBehavior();
    public static final EyeBehavior STARE = new StareBehavior();
    public static final EyeBehavior SWIRL = new SwirlBehavior();

    private static final List<EyeBehavior> ALL = List.of(
            STARE,
            BLINK,
            GROW,
            new ColorChangeBehavior(),
            SWIRL,
            SIDE_EYE,
            CROSS_EYE
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

    /** Look up a behavior by id, or {@code null} for an unregistered identifier. */
    @Nullable
    public static EyeBehavior byId(ResourceLocation id) {
        return BY_ID.get(id);
    }
}
