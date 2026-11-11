package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.OngoingBehavior;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/** Behavior settings for driving client simulations directly: the server config's built-in defaults. */
final class TestBehaviorSettings {

    /** Every behavior setting at its server-config default. */
    static final BehaviorSettings DEFAULTS = new BehaviorSettings(
            ServerConfig.AMBIENT_BEHAVIORS.defaultValue(), ServerConfig.AMBIENT_MIN_TICKS.defaultValue(),
            ServerConfig.AMBIENT_MAX_TICKS.defaultValue(), defaultPool(),
            ServerConfig.GROW_ON_HIT_PERCENT.defaultValue(), ServerConfig.SWIRL_ON_HEAL.defaultValue(),
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.defaultValue(), defaultOngoing());

    private TestBehaviorSettings() {
    }

    private static EnumSet<OngoingBehavior> defaultOngoing() {
        EnumSet<OngoingBehavior> on = EnumSet.noneOf(OngoingBehavior.class);
        for (OngoingBehavior behavior : OngoingBehavior.values()) {
            if (behavior.setting().defaultValue()) {
                on.add(behavior);
            }
        }
        return on;
    }

    private static List<EyeBehavior> defaultPool() {
        List<String> ids = ServerConfig.AMBIENT_BEHAVIOR_POOL.defaultValue();
        return Arrays.stream(EyeBehavior.values()).filter(behavior -> ids.contains(behavior.id().toString())).toList();
    }
}
