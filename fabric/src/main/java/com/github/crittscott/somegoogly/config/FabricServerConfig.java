package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Loads Fabric's per-world server TOML into the shared runtime configuration. */
public final class FabricServerConfig {

    private static final String DEFAULTS = """
            [Server Settings]
            googlyEyesEnabled = true
            globalPercent = 5
            harvestOnKillPercent = 25
            entityOverrides = []

            [Behaviors]
            ambientBehaviors = true
            ambientMinTicks = 100
            ambientMaxTicks = 400
            ambientBehaviorPool = ["somegoogly:blink", "somegoogly:cross_eye", "somegoogly:side_eye", "somegoogly:stare"]
            growOnHitPercent = 20
            swirlOnTrade = true
            swirlOnHeal = true
            swirlHealCooldownTicks = 200

            [Picker]
            allowSpawnAll = false
            """;

    private FabricServerConfig() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerConfig::load);
    }

    private static void load(MinecraftServer server) {
        ServerConfig.resetDefaults();
        Path path = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig").resolve("somegoogly-server.toml");
        try {
            Map<String, Object> values = FabricToml.readOrCreate(path, DEFAULTS);
            ServerConfig.GOOGLY_EYES_ENABLED.set(FabricToml.bool(values, "googlyEyesEnabled", true));
            ServerConfig.GLOBAL_PERCENT.set(FabricToml.integer(values, "globalPercent", 5));
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(FabricToml.integer(values, "harvestOnKillPercent", 25));
            ServerConfig.ENTITY_OVERRIDES.set(FabricToml.strings(values, "entityOverrides"));
            ServerConfig.AMBIENT_BEHAVIORS.set(FabricToml.bool(values, "ambientBehaviors", true));
            ServerConfig.AMBIENT_MIN_TICKS.set(FabricToml.integer(values, "ambientMinTicks", 100));
            ServerConfig.AMBIENT_MAX_TICKS.set(FabricToml.integer(values, "ambientMaxTicks", 400));
            ServerConfig.AMBIENT_BEHAVIOR_POOL.set(FabricToml.strings(values, "ambientBehaviorPool", List.of(
                    "somegoogly:blink", "somegoogly:cross_eye", "somegoogly:side_eye", "somegoogly:stare")));
            ServerConfig.GROW_ON_HIT_PERCENT.set(FabricToml.integer(values, "growOnHitPercent", 20));
            ServerConfig.SWIRL_ON_TRADE.set(FabricToml.bool(values, "swirlOnTrade", true));
            ServerConfig.SWIRL_ON_HEAL.set(FabricToml.bool(values, "swirlOnHeal", true));
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(
                    FabricToml.integer(values, "swirlHealCooldownTicks", 200));
            ServerConfig.ALLOW_SPAWN_ALL.set(FabricToml.bool(values, "allowSpawnAll", false));
        } catch (IOException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric server config {}", path, e);
        }
    }
}
