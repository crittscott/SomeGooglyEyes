package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/** Forge-native world server configuration backed by the shared runtime values. */
public final class ForgeServerConfig {

    private static final ForgeConfigSpec.BooleanValue GOOGLY_EYES_ENABLED;
    private static final ForgeConfigSpec.IntValue GLOBAL_PERCENT;
    private static final ForgeConfigSpec.IntValue HARVEST_ON_KILL_PERCENT;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_OVERRIDES;
    private static final ForgeConfigSpec.BooleanValue AMBIENT_BEHAVIORS;
    private static final ForgeConfigSpec.IntValue AMBIENT_MIN_TICKS;
    private static final ForgeConfigSpec.IntValue AMBIENT_MAX_TICKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMBIENT_BEHAVIOR_POOL;
    private static final ForgeConfigSpec.IntValue GROW_ON_HIT_PERCENT;
    private static final ForgeConfigSpec.BooleanValue GROW_ON_MILK;
    private static final ForgeConfigSpec.BooleanValue GROW_ON_SHEAR;
    private static final ForgeConfigSpec.BooleanValue LOVE_ON_AFFECTION;
    private static final ForgeConfigSpec.BooleanValue ANGRY_EYES;
    private static final ForgeConfigSpec.BooleanValue SQUINT_ON_SLEEP;
    private static final ForgeConfigSpec.BooleanValue SQUINT_ON_RAM;
    private static final ForgeConfigSpec.BooleanValue WINK_ON_AIM;
    private static final ForgeConfigSpec.BooleanValue RATTLE_ON_SWELL;
    private static final ForgeConfigSpec.BooleanValue PULSE_WHEN_CREEPY;
    private static final ForgeConfigSpec.BooleanValue SWIRL_ON_TRADE;
    private static final ForgeConfigSpec.BooleanValue SWIRL_ON_HEAL;
    private static final ForgeConfigSpec.IntValue SWIRL_HEAL_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_SPAWN_ALL;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPAWN_EXCLUDED_MODS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPAWN_EXCLUDED_ENTITIES;
    /** Native schema used to load and correct this loader's world server file. */
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(ServerConfig.SECTION_SERVER);
        GOOGLY_EYES_ENABLED = builder.comment(ServerConfig.GOOGLY_EYES_ENABLED_COMMENT.split("\n"))
                .define(ServerConfig.GOOGLY_EYES_ENABLED_KEY, ServerConfig.GOOGLY_EYES_ENABLED_DEFAULT);
        GLOBAL_PERCENT = builder.comment(ServerConfig.GLOBAL_PERCENT_COMMENT.split("\n"))
                .defineInRange(ServerConfig.GLOBAL_PERCENT_KEY,
                        ServerConfig.GLOBAL_PERCENT_DEFAULT, ServerConfig.PERCENT_MIN, ServerConfig.PERCENT_MAX);
        HARVEST_ON_KILL_PERCENT = builder.comment(ServerConfig.HARVEST_ON_KILL_PERCENT_COMMENT.split("\n"))
                .defineInRange(ServerConfig.HARVEST_ON_KILL_PERCENT_KEY,
                        ServerConfig.HARVEST_ON_KILL_PERCENT_DEFAULT, ServerConfig.PERCENT_MIN, ServerConfig.PERCENT_MAX);
        ENTITY_OVERRIDES = builder.comment(ServerConfig.ENTITY_OVERRIDES_COMMENT.split("\n"))
                .defineList(ServerConfig.ENTITY_OVERRIDES_KEY, ServerConfig.ENTITY_OVERRIDES_DEFAULT,
                        value -> value instanceof String string && ServerConfig.validateOverride(string));
        builder.pop().push(ServerConfig.SECTION_BEHAVIORS);
        AMBIENT_BEHAVIORS = builder.comment(ServerConfig.AMBIENT_BEHAVIORS_COMMENT.split("\n"))
                .define(ServerConfig.AMBIENT_BEHAVIORS_KEY,
                        ServerConfig.AMBIENT_BEHAVIORS_DEFAULT);
        AMBIENT_MIN_TICKS = builder.comment(ServerConfig.AMBIENT_MIN_TICKS_COMMENT.split("\n"))
                .defineInRange(ServerConfig.AMBIENT_MIN_TICKS_KEY,
                        ServerConfig.AMBIENT_MIN_TICKS_DEFAULT, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MAX);
        AMBIENT_MAX_TICKS = builder.comment(ServerConfig.AMBIENT_MAX_TICKS_COMMENT.split("\n"))
                .defineInRange(ServerConfig.AMBIENT_MAX_TICKS_KEY,
                        ServerConfig.AMBIENT_MAX_TICKS_DEFAULT, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MAX);
        AMBIENT_BEHAVIOR_POOL = builder.comment(ServerConfig.AMBIENT_BEHAVIOR_POOL_COMMENT.split("\n"))
                .defineList(
                        ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY, ServerConfig.AMBIENT_BEHAVIOR_POOL_DEFAULT,
                        value -> value instanceof String string && ServerConfig.validateResourceLocation(string));
        GROW_ON_HIT_PERCENT = builder.comment(ServerConfig.GROW_ON_HIT_PERCENT_COMMENT.split("\n"))
                .defineInRange(ServerConfig.GROW_ON_HIT_PERCENT_KEY,
                        ServerConfig.GROW_ON_HIT_PERCENT_DEFAULT, ServerConfig.PERCENT_MIN, ServerConfig.PERCENT_MAX);
        GROW_ON_MILK = builder.comment(ServerConfig.GROW_ON_MILK_COMMENT.split("\n"))
                .define(ServerConfig.GROW_ON_MILK_KEY, ServerConfig.GROW_ON_MILK_DEFAULT);
        GROW_ON_SHEAR = builder.comment(ServerConfig.GROW_ON_SHEAR_COMMENT.split("\n"))
                .define(ServerConfig.GROW_ON_SHEAR_KEY, ServerConfig.GROW_ON_SHEAR_DEFAULT);
        LOVE_ON_AFFECTION = builder.comment(ServerConfig.LOVE_ON_AFFECTION_COMMENT.split("\n"))
                .define(ServerConfig.LOVE_ON_AFFECTION_KEY, ServerConfig.LOVE_ON_AFFECTION_DEFAULT);
        ANGRY_EYES = builder.comment(ServerConfig.ANGRY_EYES_COMMENT.split("\n"))
                .define(ServerConfig.ANGRY_EYES_KEY, ServerConfig.ANGRY_EYES_DEFAULT);
        SQUINT_ON_SLEEP = builder.comment(ServerConfig.SQUINT_ON_SLEEP_COMMENT.split("\n"))
                .define(ServerConfig.SQUINT_ON_SLEEP_KEY, ServerConfig.SQUINT_ON_SLEEP_DEFAULT);
        SQUINT_ON_RAM = builder.comment(ServerConfig.SQUINT_ON_RAM_COMMENT.split("\n"))
                .define(ServerConfig.SQUINT_ON_RAM_KEY, ServerConfig.SQUINT_ON_RAM_DEFAULT);
        WINK_ON_AIM = builder.comment(ServerConfig.WINK_ON_AIM_COMMENT.split("\n"))
                .define(ServerConfig.WINK_ON_AIM_KEY, ServerConfig.WINK_ON_AIM_DEFAULT);
        RATTLE_ON_SWELL = builder.comment(ServerConfig.RATTLE_ON_SWELL_COMMENT.split("\n"))
                .define(ServerConfig.RATTLE_ON_SWELL_KEY, ServerConfig.RATTLE_ON_SWELL_DEFAULT);
        PULSE_WHEN_CREEPY = builder.comment(ServerConfig.PULSE_WHEN_CREEPY_COMMENT.split("\n"))
                .define(ServerConfig.PULSE_WHEN_CREEPY_KEY, ServerConfig.PULSE_WHEN_CREEPY_DEFAULT);
        SWIRL_ON_TRADE = builder.comment(ServerConfig.SWIRL_ON_TRADE_COMMENT.split("\n"))
                .define(ServerConfig.SWIRL_ON_TRADE_KEY, ServerConfig.SWIRL_ON_TRADE_DEFAULT);
        SWIRL_ON_HEAL = builder.comment(ServerConfig.SWIRL_ON_HEAL_COMMENT.split("\n"))
                .define(ServerConfig.SWIRL_ON_HEAL_KEY, ServerConfig.SWIRL_ON_HEAL_DEFAULT);
        SWIRL_HEAL_COOLDOWN_TICKS = builder.comment(ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_COMMENT.split("\n"))
                .defineInRange(ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_KEY,
                        ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT, ServerConfig.TICKS_MIN, ServerConfig.TICKS_MAX);
        builder.pop().push(ServerConfig.SECTION_PICKER);
        ALLOW_SPAWN_ALL = builder.comment(ServerConfig.ALLOW_SPAWN_ALL_COMMENT.split("\n"))
                .define(ServerConfig.ALLOW_SPAWN_ALL_KEY, ServerConfig.ALLOW_SPAWN_ALL_DEFAULT);
        SPAWN_EXCLUDED_MODS = builder.comment(ServerConfig.SPAWN_EXCLUDED_MODS_COMMENT.split("\n"))
                .defineList(ServerConfig.SPAWN_EXCLUDED_MODS_KEY, ServerConfig.SPAWN_EXCLUDED_MODS_DEFAULT,
                        value -> value instanceof String string && ServerConfig.validateNamespace(string));
        SPAWN_EXCLUDED_ENTITIES = builder.comment(ServerConfig.SPAWN_EXCLUDED_ENTITIES_COMMENT.split("\n"))
                .defineList(ServerConfig.SPAWN_EXCLUDED_ENTITIES_KEY, ServerConfig.SPAWN_EXCLUDED_ENTITIES_DEFAULT,
                        value -> value instanceof String string && ServerConfig.validateResourceLocation(string));
        builder.pop();
        SPEC = builder.build();
    }

    private ForgeServerConfig() {
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
        ServerConfig.GROW_ON_MILK.set(GROW_ON_MILK.get());
        ServerConfig.GROW_ON_SHEAR.set(GROW_ON_SHEAR.get());
        ServerConfig.LOVE_ON_AFFECTION.set(LOVE_ON_AFFECTION.get());
        ServerConfig.ANGRY_EYES.set(ANGRY_EYES.get());
        ServerConfig.SQUINT_ON_SLEEP.set(SQUINT_ON_SLEEP.get());
        ServerConfig.SQUINT_ON_RAM.set(SQUINT_ON_RAM.get());
        ServerConfig.WINK_ON_AIM.set(WINK_ON_AIM.get());
        ServerConfig.RATTLE_ON_SWELL.set(RATTLE_ON_SWELL.get());
        ServerConfig.PULSE_WHEN_CREEPY.set(PULSE_WHEN_CREEPY.get());
        ServerConfig.SWIRL_ON_TRADE.set(SWIRL_ON_TRADE.get());
        ServerConfig.SWIRL_ON_HEAL.set(SWIRL_ON_HEAL.get());
        ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(SWIRL_HEAL_COOLDOWN_TICKS.get());
        ServerConfig.ALLOW_SPAWN_ALL.set(ALLOW_SPAWN_ALL.get());
        ServerConfig.SPAWN_EXCLUDED_MODS.set(new ArrayList<>(SPAWN_EXCLUDED_MODS.get()));
        ServerConfig.SPAWN_EXCLUDED_ENTITIES.set(new ArrayList<>(SPAWN_EXCLUDED_ENTITIES.get()));
        ServerServices.onServerConfigApplied();
        // Only a reload can find clients connected; it arrives off the server thread from the file watcher.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (event instanceof ModConfigEvent.Reloading && server != null) {
            server.execute(() -> ServerServices.broadcastEyeConfigsIfChanged(server));
        }
    }

    /** Register the SERVER config spec and copy its values into {@link ServerConfig} on load and reload. */
    public static void register(FMLJavaModLoadingContext context) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, ServerConfig.FILE_NAME);
        context.getModEventBus().addListener(ForgeServerConfig::onConfigChanged);
    }
}
