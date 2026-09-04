package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates and loads the active world's server configuration. One {@link #SCHEMA} list drives both the
 * commented default file and the load, so adding a key means adding a single {@link Entry} rather than
 * keeping a positional template and a parallel read block in sync.
 */
public final class ServerConfigFile {

    public static final String FILE_NAME = SomeGooglyCommon.MOD_ID + "-server.toml";

    private static final String SERVER_SETTINGS = "Server Settings";
    private static final String BEHAVIORS = "Behaviors";
    private static final String PICKER = "Picker";

    private static final String ENTITY_OVERRIDES_COMMENT = """
            Per-entity eye chances, one entry per line as "entity-pattern,percent" (percent 0-100).
            '*' wildcards the entity id, e.g. "minecraft:zombie,100", "*:*_horse,50", "alexsmobs:*,0".
            An exact id always wins over a wildcard; among wildcards, the first matching line wins.
            Entities matching nothing here use globalPercent. A percent of 0 stops NEW spawns of that
            entity/pattern from rolling eyes; it does not remove eyes already granted, and a player can
            still give the entity eyes by hand with a Slimy Eye.""";

    private static final List<Entry> SCHEMA = List.of(
            bool(SERVER_SETTINGS, ServerConfig.GOOGLY_EYES_ENABLED_KEY, null,
                    ServerConfig.GOOGLY_EYES_ENABLED_DEFAULT, ServerConfig.GOOGLY_EYES_ENABLED),
            integer(SERVER_SETTINGS, ServerConfig.GLOBAL_PERCENT_KEY, null,
                    ServerConfig.GLOBAL_PERCENT_DEFAULT, ServerConfig.GLOBAL_PERCENT),
            integer(SERVER_SETTINGS, ServerConfig.HARVEST_ON_KILL_PERCENT_KEY, null,
                    ServerConfig.HARVEST_ON_KILL_PERCENT_DEFAULT, ServerConfig.HARVEST_ON_KILL_PERCENT),
            strings(SERVER_SETTINGS, ServerConfig.ENTITY_OVERRIDES_KEY, ENTITY_OVERRIDES_COMMENT,
                    ServerConfig.ENTITY_OVERRIDES_DEFAULT, ServerConfig.ENTITY_OVERRIDES),
            bool(BEHAVIORS, ServerConfig.AMBIENT_BEHAVIORS_KEY, null,
                    ServerConfig.AMBIENT_BEHAVIORS_DEFAULT, ServerConfig.AMBIENT_BEHAVIORS),
            integer(BEHAVIORS, ServerConfig.AMBIENT_MIN_TICKS_KEY, null,
                    ServerConfig.AMBIENT_MIN_TICKS_DEFAULT, ServerConfig.AMBIENT_MIN_TICKS),
            integer(BEHAVIORS, ServerConfig.AMBIENT_MAX_TICKS_KEY, null,
                    ServerConfig.AMBIENT_MAX_TICKS_DEFAULT, ServerConfig.AMBIENT_MAX_TICKS),
            strings(BEHAVIORS, ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY, null,
                    ServerConfig.AMBIENT_BEHAVIOR_POOL_DEFAULT, ServerConfig.AMBIENT_BEHAVIOR_POOL),
            integer(BEHAVIORS, ServerConfig.GROW_ON_HIT_PERCENT_KEY, null,
                    ServerConfig.GROW_ON_HIT_PERCENT_DEFAULT, ServerConfig.GROW_ON_HIT_PERCENT),
            bool(BEHAVIORS, ServerConfig.SWIRL_ON_TRADE_KEY, null,
                    ServerConfig.SWIRL_ON_TRADE_DEFAULT, ServerConfig.SWIRL_ON_TRADE),
            bool(BEHAVIORS, ServerConfig.SWIRL_ON_HEAL_KEY, null,
                    ServerConfig.SWIRL_ON_HEAL_DEFAULT, ServerConfig.SWIRL_ON_HEAL),
            integer(BEHAVIORS, ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_KEY, null,
                    ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT, ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS),
            bool(PICKER, ServerConfig.ALLOW_SPAWN_ALL_KEY, null,
                    ServerConfig.ALLOW_SPAWN_ALL_DEFAULT, ServerConfig.ALLOW_SPAWN_ALL));

    private static final String DEFAULTS = render();

    private ServerConfigFile() {
    }

    public static void load(MinecraftServer server) {
        ServerConfig.resetDefaults();
        Path path = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig").resolve(FILE_NAME);
        try {
            Map<String, Object> values = TomlConfig.readOrCreate(path, DEFAULTS);
            for (Entry entry : SCHEMA) {
                entry.apply().accept(values);
            }
        } catch (IOException e) {
            SomeGooglyCommon.LOGGER.error("Could not load server config {}", path, e);
        }
    }

    private static String render() {
        StringBuilder toml = new StringBuilder();
        String section = null;
        for (Entry entry : SCHEMA) {
            if (!entry.section().equals(section)) {
                if (section != null) {
                    toml.append('\n');
                }
                toml.append('[').append(entry.section()).append("]\n");
                section = entry.section();
            }
            if (entry.comment() != null) {
                for (String line : entry.comment().split("\n")) {
                    toml.append("# ").append(line).append('\n');
                }
            }
            toml.append(entry.key()).append(" = ").append(entry.defaultToml().get()).append('\n');
        }
        return toml.toString();
    }

    private static Entry bool(String section, String key, String comment,
                             boolean defaultValue, ConfigValue<Boolean> target) {
        return new Entry(section, key, comment, () -> String.valueOf(defaultValue),
                values -> target.set(TomlConfig.bool(values, key, defaultValue)));
    }

    private static Entry integer(String section, String key, String comment,
                                 int defaultValue, ConfigValue<Integer> target) {
        return new Entry(section, key, comment, () -> String.valueOf(defaultValue),
                values -> target.set(TomlConfig.integer(values, key, defaultValue)));
    }

    private static Entry strings(String section, String key, String comment,
                                 List<String> defaultValue, ConfigValue<List<String>> target) {
        return new Entry(section, key, comment, () -> TomlConfig.stringList(defaultValue),
                values -> target.set(TomlConfig.strings(values, key, defaultValue)));
    }

    /** One config key: where it lives in the file, how its default renders, and how a load applies it. */
    private record Entry(String section, String key, String comment,
                         Supplier<String> defaultToml, Consumer<Map<String, Object>> apply) {
    }
}
