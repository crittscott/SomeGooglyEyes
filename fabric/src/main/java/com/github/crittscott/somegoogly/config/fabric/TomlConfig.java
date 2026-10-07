package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ConfigValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Narrow TOML reader for the boolean, integer, and string-list schemas of Fabric's config files. Basic
 * ({@code "..."}) and literal ({@code '...'}) strings are understood; any other value is logged and skipped.
 */
public final class TomlConfig {

    private TomlConfig() {
    }

    public static Map<String, Object> readOrCreate(Path path, String defaults) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.writeString(path, defaults, StandardCharsets.UTF_8);
            SomeGooglyCommon.LOGGER.info("Wrote default config file {}", path);
        }
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

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

    public static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        warnMismatch(key, "integer", value, fallback);
        return fallback;
    }

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

    private static void warnMismatch(String key, String expected, Object value, Object fallback) {
        SomeGooglyCommon.LOGGER.warn(
                "Config key '{}' expected a {} but found {} ({}); using default {}",
                key, expected, value, value.getClass().getSimpleName(), fallback);
    }

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

    private static Map<String, Object> parse(String source) {
        Map<String, Object> values = new LinkedHashMap<>();
        StringBuilder statement = new StringBuilder();
        int arrayDepth = 0;
        for (String rawLine : source.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (arrayDepth == 0 && line.startsWith("[") && line.endsWith("]")) {
                continue;
            }
            if (statement.length() > 0) {
                statement.append(' ');
            }
            statement.append(line);
            arrayDepth += bracketDelta(line);
            if (arrayDepth > 0) {
                continue;
            }
            parseStatement(statement.toString(), values);
            statement.setLength(0);
            arrayDepth = 0;
        }
        if (statement.length() > 0) {
            parseStatement(statement.toString(), values);
        }
        return values;
    }

    private static void parseStatement(String statement, Map<String, Object> values) {
        int equals = statement.indexOf('=');
        if (equals < 1) {
            return;
        }
        String key = statement.substring(0, equals).trim();
        String raw = statement.substring(equals + 1).trim();
        Object value = parseValue(raw);
        if (value == null) {
            SomeGooglyCommon.LOGGER.warn("Config key '{}' has unreadable value {}; using default", key, raw);
        } else if (!key.isEmpty()) {
            values.put(key, value);
        }
    }

    private static Object parseValue(String raw) {
        if (raw.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (raw.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (raw.startsWith("[") && raw.endsWith("]")) {
            return parseStringArray(raw.substring(1, raw.length() - 1));
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> parseStringArray(String raw) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (quote == '"' && c == '\\') {
                escaped = true;
            } else if (quote == 0 && (c == '"' || c == '\'')) {
                quote = c;
            } else if (c == quote) {
                quote = 0;
            } else if (c == ',' && quote == 0) {
                addArrayEntry(values, current);
            } else {
                current.append(c);
            }
        }
        addArrayEntry(values, current);
        return values;
    }

    private static void addArrayEntry(List<String> values, StringBuilder current) {
        String value = current.toString().trim();
        current.setLength(0);
        if (!value.isEmpty()) {
            values.add(value);
        }
    }

    private static int bracketDelta(String line) {
        int delta = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (quote == '"' && c == '\\') {
                escaped = true;
            } else if (quote == 0 && (c == '"' || c == '\'')) {
                quote = c;
            } else if (c == quote) {
                quote = 0;
            } else if (quote == 0 && c == '[') {
                delta++;
            } else if (quote == 0 && c == ']') {
                delta--;
            }
        }
        return delta;
    }

    private static String stripComment(String line) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (quote == '"' && c == '\\') {
                escaped = true;
            } else if (quote == 0 && (c == '"' || c == '\'')) {
                quote = c;
            } else if (c == quote) {
                quote = 0;
            } else if (c == '#' && quote == 0) {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
