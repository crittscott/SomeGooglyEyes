package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ConfigValue;
import com.github.crittscott.somegoogly.config.ServerConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Creates and loads the active world's server TOML when the server starts and again at the start of
 * each {@code /reload}, since Fabric has no config system; values return to defaults when the server stops. One
 * {@link #SCHEMA} list drives the commented file and the load; its sections, keys, defaults, and comments match
 * the native Forge server spec. Like Forge, a file missing any key, such as one from an earlier release, is
 * rewritten with the values it holds and each missing key at its default.
 */
public final class FabricServerConfig {

    /** The per-world directory Forge keeps server configs in, mirrored here. */
    private static final String SERVER_CONFIG_DIR = "serverconfig";

    private static final List<Entry> SCHEMA = List.of(
            bool(ServerConfig.SECTION_SERVER, ServerConfig.GOOGLY_EYES_ENABLED_KEY,
                    ServerConfig.GOOGLY_EYES_ENABLED_COMMENT,
                    ServerConfig.GOOGLY_EYES_ENABLED_DEFAULT, ServerConfig.GOOGLY_EYES_ENABLED),
            integer(ServerConfig.SECTION_SERVER, ServerConfig.GLOBAL_PERCENT_KEY,
                    ServerConfig.GLOBAL_PERCENT_COMMENT,
                    ServerConfig.GLOBAL_PERCENT_DEFAULT, ServerConfig.GLOBAL_PERCENT),
            integer(ServerConfig.SECTION_SERVER, ServerConfig.HARVEST_ON_KILL_PERCENT_KEY,
                    ServerConfig.HARVEST_ON_KILL_PERCENT_COMMENT,
                    ServerConfig.HARVEST_ON_KILL_PERCENT_DEFAULT, ServerConfig.HARVEST_ON_KILL_PERCENT),
            strings(ServerConfig.SECTION_SERVER, ServerConfig.ENTITY_OVERRIDES_KEY,
                    ServerConfig.ENTITY_OVERRIDES_COMMENT,
                    ServerConfig.ENTITY_OVERRIDES_DEFAULT, ServerConfig.ENTITY_OVERRIDES),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.AMBIENT_BEHAVIORS_KEY,
                    ServerConfig.AMBIENT_BEHAVIORS_COMMENT,
                    ServerConfig.AMBIENT_BEHAVIORS_DEFAULT, ServerConfig.AMBIENT_BEHAVIORS),
            integer(ServerConfig.SECTION_BEHAVIORS, ServerConfig.AMBIENT_MIN_TICKS_KEY,
                    ServerConfig.AMBIENT_MIN_TICKS_COMMENT,
                    ServerConfig.AMBIENT_MIN_TICKS_DEFAULT, ServerConfig.AMBIENT_MIN_TICKS),
            integer(ServerConfig.SECTION_BEHAVIORS, ServerConfig.AMBIENT_MAX_TICKS_KEY,
                    ServerConfig.AMBIENT_MAX_TICKS_COMMENT,
                    ServerConfig.AMBIENT_MAX_TICKS_DEFAULT, ServerConfig.AMBIENT_MAX_TICKS),
            strings(ServerConfig.SECTION_BEHAVIORS, ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY,
                    ServerConfig.AMBIENT_BEHAVIOR_POOL_COMMENT,
                    ServerConfig.AMBIENT_BEHAVIOR_POOL_DEFAULT, ServerConfig.AMBIENT_BEHAVIOR_POOL),
            integer(ServerConfig.SECTION_BEHAVIORS, ServerConfig.GROW_ON_HIT_PERCENT_KEY,
                    ServerConfig.GROW_ON_HIT_PERCENT_COMMENT,
                    ServerConfig.GROW_ON_HIT_PERCENT_DEFAULT, ServerConfig.GROW_ON_HIT_PERCENT),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.GROW_ON_MILK_KEY,
                    ServerConfig.GROW_ON_MILK_COMMENT,
                    ServerConfig.GROW_ON_MILK_DEFAULT, ServerConfig.GROW_ON_MILK),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.GROW_ON_SHEAR_KEY,
                    ServerConfig.GROW_ON_SHEAR_COMMENT,
                    ServerConfig.GROW_ON_SHEAR_DEFAULT, ServerConfig.GROW_ON_SHEAR),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.LOVE_ON_AFFECTION_KEY,
                    ServerConfig.LOVE_ON_AFFECTION_COMMENT,
                    ServerConfig.LOVE_ON_AFFECTION_DEFAULT, ServerConfig.LOVE_ON_AFFECTION),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.ANGRY_EYES_KEY,
                    ServerConfig.ANGRY_EYES_COMMENT,
                    ServerConfig.ANGRY_EYES_DEFAULT, ServerConfig.ANGRY_EYES),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SQUINT_ON_SLEEP_KEY,
                    ServerConfig.SQUINT_ON_SLEEP_COMMENT,
                    ServerConfig.SQUINT_ON_SLEEP_DEFAULT, ServerConfig.SQUINT_ON_SLEEP),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SQUINT_ON_RAM_KEY,
                    ServerConfig.SQUINT_ON_RAM_COMMENT,
                    ServerConfig.SQUINT_ON_RAM_DEFAULT, ServerConfig.SQUINT_ON_RAM),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.WINK_ON_AIM_KEY,
                    ServerConfig.WINK_ON_AIM_COMMENT,
                    ServerConfig.WINK_ON_AIM_DEFAULT, ServerConfig.WINK_ON_AIM),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.RATTLE_ON_SWELL_KEY,
                    ServerConfig.RATTLE_ON_SWELL_COMMENT,
                    ServerConfig.RATTLE_ON_SWELL_DEFAULT, ServerConfig.RATTLE_ON_SWELL),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.PULSE_WHEN_CREEPY_KEY,
                    ServerConfig.PULSE_WHEN_CREEPY_COMMENT,
                    ServerConfig.PULSE_WHEN_CREEPY_DEFAULT, ServerConfig.PULSE_WHEN_CREEPY),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SWIRL_ON_TRADE_KEY,
                    ServerConfig.SWIRL_ON_TRADE_COMMENT,
                    ServerConfig.SWIRL_ON_TRADE_DEFAULT, ServerConfig.SWIRL_ON_TRADE),
            bool(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SWIRL_ON_HEAL_KEY,
                    ServerConfig.SWIRL_ON_HEAL_COMMENT,
                    ServerConfig.SWIRL_ON_HEAL_DEFAULT, ServerConfig.SWIRL_ON_HEAL),
            integer(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_KEY,
                    ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_COMMENT,
                    ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS_DEFAULT, ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS),
            bool(ServerConfig.SECTION_PICKER, ServerConfig.ALLOW_SPAWN_ALL_KEY,
                    ServerConfig.ALLOW_SPAWN_ALL_COMMENT,
                    ServerConfig.ALLOW_SPAWN_ALL_DEFAULT, ServerConfig.ALLOW_SPAWN_ALL),
            strings(ServerConfig.SECTION_PICKER, ServerConfig.SPAWN_EXCLUDED_MODS_KEY,
                    ServerConfig.SPAWN_EXCLUDED_MODS_COMMENT,
                    ServerConfig.SPAWN_EXCLUDED_MODS_DEFAULT, ServerConfig.SPAWN_EXCLUDED_MODS),
            strings(ServerConfig.SECTION_PICKER, ServerConfig.SPAWN_EXCLUDED_ENTITIES_KEY,
                    ServerConfig.SPAWN_EXCLUDED_ENTITIES_COMMENT,
                    ServerConfig.SPAWN_EXCLUDED_ENTITIES_DEFAULT, ServerConfig.SPAWN_EXCLUDED_ENTITIES));

    private FabricServerConfig() {
    }

    /** Load the server config at server start and each {@code /reload}, and reset it at server stop. */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerConfig::load);
        // Before the reload's eye-config broadcast, so clients receive the re-read master switch.
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resources) -> load(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerConfig.resetDefaults());
    }

    private static void load(MinecraftServer server) {
        load(server.getWorldPath(LevelResource.ROOT).resolve(SERVER_CONFIG_DIR).resolve(ServerConfig.FILE_NAME));
    }

    /**
     * Load the server config at {@code path} into {@link ServerConfig}, writing the defaults there if it is absent
     * and adding any keys it lacks.
     */
    public static void load(Path path) {
        ServerConfig.resetDefaults();
        try {
            Map<String, Object> values = TomlConfig.readOrCreate(path, render());
            for (Entry entry : SCHEMA) {
                entry.apply().accept(values);
            }
            TomlConfig.completeIfMissing(path, values, SCHEMA.stream().map(Entry::key).toList(), render());
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
                toml.append(TomlConfig.table(entry.section())).append('\n');
                section = entry.section();
            }
            if (entry.comment() != null) {
                for (String line : entry.comment().split("\n")) {
                    toml.append("# ").append(line).append('\n');
                }
            }
            toml.append(entry.key()).append(" = ").append(entry.toml().get()).append('\n');
        }
        return toml.toString();
    }

    private static Entry bool(String section, String key, String comment,
                             boolean defaultValue, ConfigValue<Boolean> target) {
        return new Entry(section, key, comment, () -> String.valueOf(target.get()),
                values -> target.set(TomlConfig.bool(values, key, defaultValue)));
    }

    private static Entry integer(String section, String key, String comment,
                                 int defaultValue, ConfigValue<Integer> target) {
        return new Entry(section, key, comment, () -> String.valueOf(target.get()),
                values -> TomlConfig.assign(target, key, TomlConfig.integer(values, key, defaultValue)));
    }

    private static Entry strings(String section, String key, String comment,
                                 List<String> defaultValue, ConfigValue<List<String>> target) {
        return new Entry(section, key, comment, () -> TomlConfig.stringList(target.get()),
                values -> TomlConfig.assign(target, key, TomlConfig.strings(values, key, defaultValue)));
    }

    /** One config key: where it lives in the file, how its current value renders, and how a load applies it. */
    private record Entry(String section, String key, String comment,
                         Supplier<String> toml, Consumer<Map<String, Object>> apply) {
    }
}
