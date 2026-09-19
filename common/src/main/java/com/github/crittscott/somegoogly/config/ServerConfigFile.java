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
 * Creates and loads Fabric and NeoForge's active-world server configuration. One {@link #SCHEMA} list drives both
 * the commented default file and the load; its keys, defaults, ranges, and validators must stay aligned
 * with the native Forge server spec.
 */
public final class ServerConfigFile {

    public static final String FILE_NAME = SomeGooglyCommon.MOD_ID + "-server.toml";

    private static final String SERVER_SETTINGS = "server";
    private static final String BEHAVIORS = "behaviors";
    private static final String PICKER = "picker";

    private static final String GOOGLY_EYES_ENABLED_COMMENT = """
            Master switch. false stops new mobs from rolling eyes, hides every eye (old and new) on every
            client, and refuses hand-applying or harvesting eyes; existing NBT eye data is left untouched
            and reappears when this is turned back on. Already-connected clients only see the change after
            a config reload or server restart, since this is a server config value like any other.""";

    private static final String ENTITY_OVERRIDES_COMMENT = """
            Per-entity eye chances, one entry per line as "entity-pattern,percent" (percent 0-100).
            '*' wildcards the entity id, e.g. "minecraft:zombie,100", "*:*_horse,50", "alexsmobs:*,0".
            An exact id always wins over a wildcard; among wildcards, the first matching line wins.
            Entities matching nothing here use globalPercent. A percent of 0 stops NEW spawns of that
            entity/pattern from rolling eyes; it does not remove eyes already granted, and a player can
            still give the entity eyes by hand with a Slimy Eye.""";

    private static final String ALLOW_SPAWN_ALL_COMMENT = """
             Enables /sg spawnall, which force-spawns every summonable living mob in a grid with no undo.
             WARNING: only enable this on a test or throwaway world. Spawning a mob outside its own mod's
             normal context, as spawnall does, can corrupt or destabilize the world; MineColonies and
             Create mobs are known cases (see spawnExcludedMods below).""";

    private static final String SPAWN_EXCLUDED_MODS_COMMENT = """
             Namespaces that /sg spawn and /sg spawnall skip, one quoted entry per line, e.g. "mekanism".
             MineColonies and Create are excluded by default because their mobs can corrupt or
             destabilize a world if force-spawned outside their mod's normal context. Only remove either
             entry on a world you're prepared to lose. These are authoring commands only; nothing here
             changes eye eligibility or natural spawning.""";

    private static final String SPAWN_EXCLUDED_ENTITIES_COMMENT = """
            Entity ids that /sg spawn and /sg spawnall skip, one quoted entry per line, e.g.
            "minecraft:armor_stand". Same authoring-only scope as spawnExcludedMods.""";

    private static final List<Entry> SCHEMA = List.of(
            bool(SERVER_SETTINGS, ServerConfig.GOOGLY_EYES_ENABLED_KEY, GOOGLY_EYES_ENABLED_COMMENT,
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
            bool(PICKER, ServerConfig.ALLOW_SPAWN_ALL_KEY, ALLOW_SPAWN_ALL_COMMENT,
                    ServerConfig.ALLOW_SPAWN_ALL_DEFAULT, ServerConfig.ALLOW_SPAWN_ALL),
            strings(PICKER, ServerConfig.SPAWN_EXCLUDED_MODS_KEY, SPAWN_EXCLUDED_MODS_COMMENT,
                    ServerConfig.SPAWN_EXCLUDED_MODS_DEFAULT, ServerConfig.SPAWN_EXCLUDED_MODS),
            strings(PICKER, ServerConfig.SPAWN_EXCLUDED_ENTITIES_KEY, SPAWN_EXCLUDED_ENTITIES_COMMENT,
                    ServerConfig.SPAWN_EXCLUDED_ENTITIES_DEFAULT, ServerConfig.SPAWN_EXCLUDED_ENTITIES));

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
