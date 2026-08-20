package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

/** Forge TOML storage for the shared {@link ServerConfig} runtime values. */
public final class ForgeServerConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue ALLOW_SPAWN_ALL;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMBIENT_BEHAVIOR_POOL;
    private static final ForgeConfigSpec.BooleanValue AMBIENT_BEHAVIORS;
    private static final ForgeConfigSpec.IntValue AMBIENT_MAX_TICKS;
    private static final ForgeConfigSpec.IntValue AMBIENT_MIN_TICKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_OVERRIDES;
    private static final ForgeConfigSpec.IntValue GLOBAL_PERCENT;
    private static final ForgeConfigSpec.BooleanValue GOOGLY_EYES_ENABLED;
    private static final ForgeConfigSpec.IntValue GROW_ON_HIT_PERCENT;
    private static final ForgeConfigSpec.IntValue HARVEST_ON_KILL_PERCENT;
    private static final ForgeConfigSpec.IntValue SWIRL_HEAL_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.BooleanValue SWIRL_ON_HEAL;
    private static final ForgeConfigSpec.BooleanValue SWIRL_ON_TRADE;
    private static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("Server Settings");
        GOOGLY_EYES_ENABLED = BUILDER.comment("Enable googly eyes globally")
                .define("googlyEyesEnabled", true);
        GLOBAL_PERCENT = BUILDER.comment("Default percentage chance for an eligible entity to get googly eyes")
                .defineInRange("globalPercent", 5, 0, 100);
        HARVEST_ON_KILL_PERCENT = BUILDER.comment("Percentage chance for an eyed mob killed with shears to drop an eye")
                .defineInRange("harvestOnKillPercent", 25, 0, 100);
        ENTITY_OVERRIDES = BUILDER.comment("Per-entity chances as 'entity-pattern,percent'")
                .defineList("entityOverrides", ArrayList::new,
                        value -> value instanceof String text && ServerConfig.validateOverride(text));
        BUILDER.pop();

        BUILDER.push("Behaviors");
        AMBIENT_BEHAVIORS = BUILDER.define("ambientBehaviors", true);
        AMBIENT_MIN_TICKS = BUILDER.defineInRange("ambientMinTicks", 100, 1, 24000);
        AMBIENT_MAX_TICKS = BUILDER.defineInRange("ambientMaxTicks", 400, 1, 24000);
        AMBIENT_BEHAVIOR_POOL = BUILDER.defineList("ambientBehaviorPool", List.of(
                        "somegoogly:blink", "somegoogly:cross_eye", "somegoogly:side_eye", "somegoogly:stare"),
                value -> value instanceof String text && ServerConfig.validateBehaviorId(text));
        GROW_ON_HIT_PERCENT = BUILDER.defineInRange("growOnHitPercent", 20, 0, 100);
        SWIRL_ON_TRADE = BUILDER.define("swirlOnTrade", true);
        SWIRL_ON_HEAL = BUILDER.define("swirlOnHeal", true);
        SWIRL_HEAL_COOLDOWN_TICKS = BUILDER.defineInRange("swirlHealCooldownTicks", 200, 1, 24000);
        BUILDER.pop();

        BUILDER.push("Picker");
        ALLOW_SPAWN_ALL = BUILDER.define("allowSpawnAll", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ForgeServerConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeServerConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
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
