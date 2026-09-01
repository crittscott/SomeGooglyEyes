package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** Creates and loads the active world's server configuration. */
public final class ServerConfigFile {

    public static final String FILE_NAME = SomeGooglyCommon.MOD_ID + "-server.toml";

    private static final String DEFAULTS = """
            [Server Settings]
            %s = %s
            %s = %s
            %s = %s
            # Per-entity eye chances, one entry per line as "entity-pattern,percent" (percent 0-100).
            # '*' wildcards the entity id, e.g. "minecraft:zombie,100", "*:*_horse,50", "alexsmobs:*,0".
            # An exact id always wins over a wildcard; among wildcards, the first matching line wins.
            # Entities matching nothing here use globalPercent. A percent of 0 stops NEW spawns of that
            # entity/pattern from rolling eyes; it does not remove eyes already granted, and a player can
            # still give the entity eyes by hand with a Slimy Eye.
            %s = %s

            [Behaviors]
            %s = %s
            %s = %s
            %s = %s
            %s = %s
            %s = %s
            %s = %s
            %s = %s
            %s = %s

            [Picker]
            %s = %s
            """.formatted(
            ServerConfig.GOOGLY_EYES_ENABLED_KEY, ServerConfig.GOOGLY_EYES_ENABLED_DEFAULT,
            ServerConfig.GLOBAL_PERCENT_KEY, ServerConfig.GLOBAL_PERCENT_DEFAULT,
            ServerConfig.HARVEST_ON_KILL_PERCENT_KEY, ServerConfig.HARVEST_ON_KILL_PERCENT_DEFAULT,
            ServerConfig.ENTITY_OVERRIDES_KEY, TomlConfig.stringList(ServerConfig.ENTITY_OVERRIDES_DEFAULT),
            ServerConfig.AMBIENT_BEHAVIORS_KEY, ServerConfig.AMBIENT_BEHAVIORS_DEFAULT,
            ServerConfig.AMBIENT_MIN_TICKS_KEY, ServerConfig.AMBIENT_MIN_TICKS_DEFAULT,
            ServerConfig.AMBIENT_MAX_TICKS_KEY, ServerConfig.AMBIENT_MAX_TICKS_DEFAULT,
            ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY,
            TomlConfig.stringList(ServerConfig.AMBIENT_BEHAVIOR_POOL_DEFAULT),
            ServerConfig.GROW_ON_HIT_PERCENT_KEY, ServerConfig.GROW_ON_HIT_PERCENT_DEFAULT,
            ServerConfig.SWIRL_ON_TRADE_KEY, ServerConfig.SWIRL_ON_TRADE_DEFAULT,
            ServerConfig.SWIRL_ON_HEAL_KEY, ServerConfig.SWIRL_ON_HEAL_DEFAULT,
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_KEY, ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT,
            ServerConfig.ALLOW_SPAWN_ALL_KEY, ServerConfig.ALLOW_SPAWN_ALL_DEFAULT);

    private ServerConfigFile() {
    }

    public static void load(MinecraftServer server) {
        ServerConfig.resetDefaults();
        Path path = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig").resolve(FILE_NAME);
        try {
            Map<String, Object> values = TomlConfig.readOrCreate(path, DEFAULTS);
            ServerConfig.GOOGLY_EYES_ENABLED.set(TomlConfig.bool(values,
                    ServerConfig.GOOGLY_EYES_ENABLED_KEY, ServerConfig.GOOGLY_EYES_ENABLED_DEFAULT));
            ServerConfig.GLOBAL_PERCENT.set(TomlConfig.integer(values,
                    ServerConfig.GLOBAL_PERCENT_KEY, ServerConfig.GLOBAL_PERCENT_DEFAULT));
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(TomlConfig.integer(values,
                    ServerConfig.HARVEST_ON_KILL_PERCENT_KEY, ServerConfig.HARVEST_ON_KILL_PERCENT_DEFAULT));
            ServerConfig.ENTITY_OVERRIDES.set(TomlConfig.strings(values,
                    ServerConfig.ENTITY_OVERRIDES_KEY, ServerConfig.ENTITY_OVERRIDES_DEFAULT));
            ServerConfig.AMBIENT_BEHAVIORS.set(TomlConfig.bool(values,
                    ServerConfig.AMBIENT_BEHAVIORS_KEY, ServerConfig.AMBIENT_BEHAVIORS_DEFAULT));
            ServerConfig.AMBIENT_MIN_TICKS.set(TomlConfig.integer(values,
                    ServerConfig.AMBIENT_MIN_TICKS_KEY, ServerConfig.AMBIENT_MIN_TICKS_DEFAULT));
            ServerConfig.AMBIENT_MAX_TICKS.set(TomlConfig.integer(values,
                    ServerConfig.AMBIENT_MAX_TICKS_KEY, ServerConfig.AMBIENT_MAX_TICKS_DEFAULT));
            ServerConfig.AMBIENT_BEHAVIOR_POOL.set(TomlConfig.strings(values,
                    ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY, ServerConfig.AMBIENT_BEHAVIOR_POOL_DEFAULT));
            ServerConfig.GROW_ON_HIT_PERCENT.set(TomlConfig.integer(values,
                    ServerConfig.GROW_ON_HIT_PERCENT_KEY, ServerConfig.GROW_ON_HIT_PERCENT_DEFAULT));
            ServerConfig.SWIRL_ON_TRADE.set(TomlConfig.bool(values,
                    ServerConfig.SWIRL_ON_TRADE_KEY, ServerConfig.SWIRL_ON_TRADE_DEFAULT));
            ServerConfig.SWIRL_ON_HEAL.set(TomlConfig.bool(values,
                    ServerConfig.SWIRL_ON_HEAL_KEY, ServerConfig.SWIRL_ON_HEAL_DEFAULT));
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(TomlConfig.integer(values,
                    ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_KEY,
                    ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT));
            ServerConfig.ALLOW_SPAWN_ALL.set(TomlConfig.bool(values,
                    ServerConfig.ALLOW_SPAWN_ALL_KEY, ServerConfig.ALLOW_SPAWN_ALL_DEFAULT));
        } catch (IOException e) {
            SomeGooglyCommon.LOGGER.error("Could not load server config {}", path, e);
        }
    }
}
