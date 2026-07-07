package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGoogly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Minimal matcher for exact versions and bracket ranges such as [1.2.0,1.3.0), plus the
 * nearest-generation fallback pick ({@link #nearestVersion}) used when no entry matches at all.
 */
public final class VersionRangeMatcher {

    private VersionRangeMatcher() {
    }

    /** A declaration's endpoints; {@code null} = unbounded on that side. An exact version is a point. */
    private record Bounds(@Nullable String lower, @Nullable String upper) {
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

    /** Parse a declared version into its endpoints, or {@code null} if malformed (same cases as {@link #matches}). */
    @Nullable
    private static Bounds bounds(String range) {
        if (range == null || range.isBlank()) {
            return null;
        }
        String trimmed = range.trim();
        if (!(trimmed.startsWith("[") || trimmed.startsWith("("))) {
            return new Bounds(trimmed, trimmed);
        }
        if (!(trimmed.endsWith("]") || trimmed.endsWith(")"))) {
            return null;
        }
        String body = trimmed.substring(1, trimmed.length() - 1);
        int comma = body.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String lower = body.substring(0, comma).trim();
        String upper = body.substring(comma + 1).trim();
        return new Bounds(lower.isEmpty() ? null : lower, upper.isEmpty() ? null : upper);
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

    /**
     * Whether the whole declared range sits at or below {@code version} (its upper bound does).
     * Distinguishes the fallback log levels: a {@link #nearestVersion} pick that is entirely below the
     * installed version means the datapack is stale (older than the mod); otherwise the mod was
     * downgraded below every declaration.
     */
    public static boolean isEntirelyBelow(String range, String version) {
        Bounds b = bounds(range);
        return b != null && b.upper() != null && compare(b.upper(), version) <= 0;
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

    /**
     * The fallback pick for when <b>no</b> declaration matches the installed version: the declared
     * version string nearest to {@code loadedVersion} — the newest declaration entirely older than it,
     * or, when the installed version predates them all, the oldest declaration. Version ordering has no
     * distance metric, so "nearest" is defined by ordering alone; a gap between two generations resolves
     * to the older neighbor (its model at least existed when the entry was authored). Malformed
     * declarations are skipped, matching {@link #matches}. Returns one of the inputs verbatim, or
     * {@code null} when nothing is usable.
     */
    @Nullable
    public static String nearestVersion(Collection<String> declaredVersions, String loadedVersion) {
        if (loadedVersion == null || loadedVersion.isBlank()) {
            return null;
        }
        String bestOlder = null;  // greatest upper bound at or below the installed version
        String bestOlderUpper = null;
        String bestNewer = null;  // smallest lower bound at or above the installed version
        String bestNewerLower = null;
        for (String declared : declaredVersions) {
            Bounds b = bounds(declared);
            if (b == null) {
                continue;
            }
            if (b.upper() != null && compare(b.upper(), loadedVersion) <= 0) {
                if (bestOlderUpper == null || compare(b.upper(), bestOlderUpper) > 0) {
                    bestOlder = declared;
                    bestOlderUpper = b.upper();
                }
            } else if (b.lower() != null && compare(b.lower(), loadedVersion) >= 0) {
                if (bestNewerLower == null || compare(b.lower(), bestNewerLower) < 0) {
                    bestNewer = declared;
                    bestNewerLower = b.lower();
                }
            }
            // Neither branch: the range brackets the installed version, so it would have matched —
            // unreachable for the non-matching declarations this is called with. Skipped defensively.
        }
        return bestOlder != null ? bestOlder : bestNewer;
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
