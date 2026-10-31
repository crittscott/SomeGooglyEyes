package com.github.crittscott.somegoogly.config.fabric;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ConfigValue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fabric configuration lifecycle and schema conversions over NightConfig's TOML parser. */
public final class TomlConfig {

    private TomlConfig() {
    }

    private static boolean addMissing(CommentedConfig values, CommentedConfig defaults) {
        boolean changed = false;
        for (CommentedConfig.Entry entry : defaults.entrySet()) {
            List<String> key = List.of(entry.getKey());
            Object existing = values.get(key);
            Object fallback = entry.getValue();
            if (!values.contains(key)) {
                values.set(key, fallback);
                values.setComment(key, entry.getComment());
                changed = true;
            } else if (existing instanceof CommentedConfig table && fallback instanceof CommentedConfig defaultTable) {
                changed |= addMissing(table, defaultTable);
            }
        }
        return changed;
    }

    /** Assign {@code raw} to {@code target}, logging when its range clamps the value. */
    public static void assign(ConfigValue<Integer> target, String key, int raw) {
        target.set(raw);
        if (target.get() != raw) {
            SomeGooglyCommon.LOGGER.warn("Config key '{}' value {} is out of range; using {}", key, raw, target.get());
        }
    }

    /** Assign {@code raw} to {@code target}, logging each entry its validator rejects. */
    public static void assign(ConfigValue<List<String>> target, String key, List<String> raw) {
        target.set(raw);
        List<String> accepted = target.get();
        if (accepted.size() == raw.size()) {
            return;
        }
        for (String entry : raw) {
            if (!accepted.contains(entry)) {
                SomeGooglyCommon.LOGGER.warn("Config key '{}' has an invalid entry '{}'; ignoring it", key, entry);
            }
        }
    }

    /** The boolean at {@code key}, or {@code fallback} when absent or, logged, of another type. */
    public static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        warnMismatch(key, "boolean", value, fallback);
        return fallback;
    }

    /**
     * Add only absent schema values, retaining existing values, unknown keys, and parsed comments. The
     * original text is retained in a sibling {@code .bak} file before completion reformats the document.
     */
    public static boolean completeIfMissing(Path path, CommentedConfig values, String defaults) throws IOException {
        if (!addMissing(values, parse(defaults))) {
            return false;
        }
        String completed = new TomlWriter().writeToString(values);
        Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(path, completed, StandardCharsets.UTF_8);
        SomeGooglyCommon.LOGGER.info("Added missing keys to config file {} (original retained in .bak)", path);
        return true;
    }

    /** The integer at {@code key}, or {@code fallback} when absent or, logged, of another type. */
    public static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long integer && integer >= Integer.MIN_VALUE && integer <= Integer.MAX_VALUE) {
            return integer.intValue();
        }
        warnMismatch(key, "integer", value, fallback);
        return fallback;
    }

    private static CommentedConfig parse(String source) throws IOException {
        CommentedConfig config = TomlFormat.newConfig(LinkedHashMap::new);
        try {
            new TomlParser().parse(new StringReader(source), config, ParsingMode.REPLACE);
        } catch (ParsingException e) {
            throw new IOException("Invalid TOML: " + e.getMessage(), e);
        }
        return config;
    }

    /** Parse a whole file before applying any runtime values; a parse failure leaves its text untouched. */
    public static CommentedConfig readOrCreate(Path path, String defaults) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.writeString(path, defaults, StandardCharsets.UTF_8);
            SomeGooglyCommon.LOGGER.info("Wrote default config file {}", path);
        }
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    /** Values from exactly the schema's declared table; similarly named keys elsewhere do not apply. */
    public static Map<String, Object> section(UnmodifiableConfig values, String section) {
        Object table = values.get(List.of(section));
        if (table == null) {
            return Map.of();
        }
        if (!(table instanceof UnmodifiableConfig config)) {
            throw new IllegalArgumentException("Config section is not a table: " + section);
        }
        return config.valueMap();
    }

    /** Render {@code values} as a TOML array of escaped basic strings, for writing config files. */
    public static String stringList(List<String> values) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append('"')
                    .append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"');
        }
        return result.append(']').toString();
    }

    /**
     * The string array at {@code key}, or {@code fallback} when absent or, logged, not an array. Non-string
     * entries are logged and dropped.
     */
    public static List<String> strings(Map<String, Object> values, String key, List<String> fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> list)) {
            warnMismatch(key, "string array", value, fallback);
            return fallback;
        }
        List<String> strings = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String string) {
                strings.add(string);
            } else {
                SomeGooglyCommon.LOGGER.warn(
                        "Config key '{}' has a non-string array entry {} ({}); ignoring it",
                        key, entry, entry == null ? "null" : entry.getClass().getSimpleName());
            }
        }
        return strings;
    }

    /** Render a table header for {@code name}, quoting it when it isn't a bare TOML key, as Forge does. */
    public static String table(String name) {
        return name.matches("[A-Za-z0-9_-]+") ? "[" + name + "]" : "[\"" + name + "\"]";
    }

    private static void warnMismatch(String key, String expected, Object value, Object fallback) {
        SomeGooglyCommon.LOGGER.warn(
                "Config key '{}' expected a {} but found {} ({}); using default {}",
                key, expected, value, value.getClass().getSimpleName(), fallback);
    }
}
