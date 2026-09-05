package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** NeoForge-native world server configuration backed by the shared runtime values. */
public final class NeoForgeServerConfig {

    private static final ModConfigSpec.BooleanValue GOOGLY_EYES_ENABLED;
    private static final ModConfigSpec.IntValue GLOBAL_PERCENT;
    private static final ModConfigSpec.IntValue HARVEST_ON_KILL_PERCENT;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_OVERRIDES;
    private static final ModConfigSpec.BooleanValue AMBIENT_BEHAVIORS;
    private static final ModConfigSpec.IntValue AMBIENT_MIN_TICKS;
    private static final ModConfigSpec.IntValue AMBIENT_MAX_TICKS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> AMBIENT_BEHAVIOR_POOL;
    private static final ModConfigSpec.IntValue GROW_ON_HIT_PERCENT;
    private static final ModConfigSpec.BooleanValue SWIRL_ON_TRADE;
    private static final ModConfigSpec.BooleanValue SWIRL_ON_HEAL;
    private static final ModConfigSpec.IntValue SWIRL_HEAL_COOLDOWN_TICKS;
    private static final ModConfigSpec.BooleanValue ALLOW_SPAWN_ALL;
    private static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("server");
        GOOGLY_EYES_ENABLED = builder.define(ServerConfig.GOOGLY_EYES_ENABLED_KEY,
                ServerConfig.GOOGLY_EYES_ENABLED_DEFAULT);
        GLOBAL_PERCENT = builder.defineInRange(ServerConfig.GLOBAL_PERCENT_KEY,
                ServerConfig.GLOBAL_PERCENT_DEFAULT, ServerConfig.PERCENT_MIN, ServerConfig.PERCENT_MAX);
        HARVEST_ON_KILL_PERCENT = builder.defineInRange(ServerConfig.HARVEST_ON_KILL_PERCENT_KEY,
                ServerConfig.HARVEST_ON_KILL_PERCENT_DEFAULT, ServerConfig.PERCENT_MIN, ServerConfig.PERCENT_MAX);
        ENTITY_OVERRIDES = builder.comment(
                        "Per-entity eye chances as entity-pattern,percent; exact ids beat the first matching wildcard.")
                .defineList(ServerConfig.ENTITY_OVERRIDES_KEY, ServerConfig.ENTITY_OVERRIDES_DEFAULT, () -> "",
                        value -> value instanceof String string && ServerConfig.validateOverride(string));
        builder.pop().push("behaviors");
        AMBIENT_BEHAVIORS = builder.define(ServerConfig.AMBIENT_BEHAVIORS_KEY,
                ServerConfig.AMBIENT_BEHAVIORS_DEFAULT);
        AMBIENT_MIN_TICKS = builder.defineInRange(ServerConfig.AMBIENT_MIN_TICKS_KEY,
                ServerConfig.AMBIENT_MIN_TICKS_DEFAULT, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MAX);
        AMBIENT_MAX_TICKS = builder.defineInRange(ServerConfig.AMBIENT_MAX_TICKS_KEY,
                ServerConfig.AMBIENT_MAX_TICKS_DEFAULT, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MAX);
        AMBIENT_BEHAVIOR_POOL = builder.defineList(
                ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY, ServerConfig.AMBIENT_BEHAVIOR_POOL_DEFAULT, () -> "",
                value -> value instanceof String string && ServerConfig.validateBehaviorId(string));
        GROW_ON_HIT_PERCENT = builder.defineInRange(ServerConfig.GROW_ON_HIT_PERCENT_KEY,
                ServerConfig.GROW_ON_HIT_PERCENT_DEFAULT, ServerConfig.PERCENT_MIN, ServerConfig.PERCENT_MAX);
        SWIRL_ON_TRADE = builder.define(ServerConfig.SWIRL_ON_TRADE_KEY, ServerConfig.SWIRL_ON_TRADE_DEFAULT);
        SWIRL_ON_HEAL = builder.define(ServerConfig.SWIRL_ON_HEAL_KEY, ServerConfig.SWIRL_ON_HEAL_DEFAULT);
        SWIRL_HEAL_COOLDOWN_TICKS = builder.defineInRange(ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_KEY,
                ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MAX);
        builder.pop().push("picker");
        ALLOW_SPAWN_ALL = builder.define(ServerConfig.ALLOW_SPAWN_ALL_KEY, ServerConfig.ALLOW_SPAWN_ALL_DEFAULT);
        builder.pop();
        SPEC = builder.build();
    }

    private NeoForgeServerConfig() {
    }

    public static void register(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, SomeGooglyCommon.MOD_ID + "-server.toml");
        modBus.addListener(NeoForgeServerConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        if (event instanceof ModConfigEvent.Unloading) {
            ServerConfig.resetDefaults();
            return;
        }
        ServerConfig.GOOGLY_EYES_ENABLED.set(GOOGLY_EYES_ENABLED.get());
        ServerConfig.GLOBAL_PERCENT.set(GLOBAL_PERCENT.get());
        ServerConfig.HARVEST_ON_KILL_PERCENT.set(HARVEST_ON_KILL_PERCENT.get());
        ServerConfig.ENTITY_OVERRIDES.set(new ArrayList<>(ENTITY_OVERRIDES.get()));
        ServerConfig.AMBIENT_BEHAVIORS.set(AMBIENT_BEHAVIORS.get());
        ServerConfig.AMBIENT_MIN_TICKS.set(AMBIENT_MIN_TICKS.get());
        ServerConfig.AMBIENT_MAX_TICKS.set(AMBIENT_MAX_TICKS.get());
        ServerConfig.AMBIENT_BEHAVIOR_POOL.set(new ArrayList<>(AMBIENT_BEHAVIOR_POOL.get()));
        ServerConfig.GROW_ON_HIT_PERCENT.set(GROW_ON_HIT_PERCENT.get());
        ServerConfig.SWIRL_ON_TRADE.set(SWIRL_ON_TRADE.get());
        ServerConfig.SWIRL_ON_HEAL.set(SWIRL_ON_HEAL.get());
        ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(SWIRL_HEAL_COOLDOWN_TICKS.get());
        ServerConfig.ALLOW_SPAWN_ALL.set(ALLOW_SPAWN_ALL.get());
    }
}
