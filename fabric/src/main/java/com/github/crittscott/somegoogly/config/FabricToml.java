package com.github.crittscott.somegoogly.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Narrow TOML reader for this mod's boolean, integer, and string-list configuration schema. */
final class FabricToml {

    private FabricToml() {
    }

    static Map<String, Object> readOrCreate(Path path, String defaults) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.writeString(path, defaults, StandardCharsets.UTF_8);
        }
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        return value instanceof Integer integer ? integer : fallback;
    }

    static List<String> strings(Map<String, Object> values, String key) {
        return strings(values, key, List.of());
    }

    static List<String> strings(Map<String, Object> values, String key, List<String> fallback) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) {
            return fallback;
        }
        List<String> strings = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String string) {
                strings.add(string);
            }
        }
        return strings;
    }

    private static Map<String, Object> parse(String source) {
        Map<String, Object> values = new LinkedHashMap<>();
        StringBuilder statement = new StringBuilder();
        int arrayDepth = 0;
        for (String rawLine : source.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty() || line.startsWith("[")) {
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
        if (!key.isEmpty() && value != null) {
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
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (quoted && c == '\\') {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
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
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (quoted && c == '\\') {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (!quoted && c == '[') {
                delta++;
            } else if (!quoted && c == ']') {
                delta--;
            }
        }
        return delta;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (quoted && c == '\\') {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
