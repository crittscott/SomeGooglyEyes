package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGoogly;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minimal matcher for exact versions and bracket ranges such as [1.2.0,1.3.0). */
public final class VersionRangeMatcher {

    private VersionRangeMatcher() {
    }

    private record Token(Integer number, String text) implements Comparable<Token> {
        private static final Token ZERO = new Token(0, null);

        @Override
        public int compareTo(Token other) {
            if (number != null && other.number != null) {
                return Integer.compare(number, other.number);
            }
            if (number != null) {
                return 1;
            }
            if (other.number != null) {
                return -1;
            }
            return text.compareTo(other.text);
        }

        static Token of(String value) {
            try {
                return new Token(Integer.parseInt(value), null);
            } catch (NumberFormatException ignored) {
                return new Token(null, value);
            }
        }
    }

    private static int compare(String left, String right) {
        List<Token> a = tokenize(left);
        List<Token> b = tokenize(right);
        int max = Math.max(a.size(), b.size());
        for (int i = 0; i < max; i++) {
            Token ta = i < a.size() ? a.get(i) : Token.ZERO;
            Token tb = i < b.size() ? b.get(i) : Token.ZERO;
            int cmp = ta.compareTo(tb);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public static boolean matches(String range, String version) {
        if (range == null || range.isBlank() || version == null || version.isBlank()) {
            return false;
        }

        String trimmed = range.trim();
        if (!(trimmed.startsWith("[") || trimmed.startsWith("("))) {
            return trimmed.equals(version);
        }

        if (!(trimmed.endsWith("]") || trimmed.endsWith(")"))) {
            warnInvalid(range);
            return false;
        }

        String body = trimmed.substring(1, trimmed.length() - 1);
        int comma = body.indexOf(',');
        if (comma < 0) {
            warnInvalid(range);
            return false;
        }

        String lower = body.substring(0, comma).trim();
        String upper = body.substring(comma + 1).trim();
        boolean includeLower = trimmed.startsWith("[");
        boolean includeUpper = trimmed.endsWith("]");

        if (!lower.isEmpty()) {
            int cmp = compare(version, lower);
            if (cmp < 0 || (cmp == 0 && !includeLower)) {
                return false;
            }
        }
        if (!upper.isEmpty()) {
            int cmp = compare(version, upper);
            if (cmp > 0 || (cmp == 0 && !includeUpper)) {
                return false;
            }
        }
        return true;
    }

    private static List<Token> tokenize(String version) {
        List<Token> tokens = new ArrayList<>();
        for (String part : version.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!part.isEmpty()) {
                tokens.add(Token.of(part));
            }
        }
        return tokens;
    }

    private static void warnInvalid(String range) {
        SomeGoogly.LOGGER.warn("Invalid SomeGoogly version range '{}'; entry will not match", range);
    }
}
