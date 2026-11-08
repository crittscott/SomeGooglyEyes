package com.github.crittscott.somegoogly.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * One configuration setting: everything each loader needs to declare it in its config file, plus the mutable,
 * validated runtime view of the value that file holds. {@link Flag}, {@link Ranged}, and {@link Parsed} are the
 * kinds a loader's file distinguishes.
 */
public abstract class ConfigValue<T> {

    private final String comment;
    private final T defaultValue;
    private final String key;
    private final UnaryOperator<T> normalize;
    private final String section;
    private volatile T value;

    private ConfigValue(String section, String key, T defaultValue, UnaryOperator<T> normalize, String comment) {
        this.section = section;
        this.key = key;
        this.comment = comment;
        this.normalize = normalize;
        this.defaultValue = normalize.apply(defaultValue);
        this.value = this.defaultValue;
    }

    /** Create a boolean value; no normalization is applied. */
    public static Flag bool(String section, String key, boolean defaultValue, String comment) {
        return new Flag(section, key, defaultValue, comment);
    }

    /** The file comment, one line per comment line. */
    public String comment() {
        return comment;
    }

    /** The (already normalized) factory default. */
    public T defaultValue() {
        return defaultValue;
    }

    private static UnaryOperator<List<String>> filter(Predicate<String> valid) {
        return values -> {
            List<String> accepted = new ArrayList<>();
            for (String value : values) {
                if (valid.test(value)) {
                    accepted.add(value);
                }
            }
            return List.copyOf(accepted);
        };
    }

    /** The current normalized value. */
    public T get() {
        return value;
    }

    /**
     * Create an integer value whose default and every subsequently assigned value are clamped to
     * {@code [min, max]}.
     */
    public static Ranged integer(String section, String key, int defaultValue, int min, int max, String comment) {
        return new Ranged(section, key, defaultValue, min, max, comment);
    }

    /** The key within {@link #section}. */
    public String key() {
        return key;
    }

    /**
     * Create a string-list value whose default and every subsequently assigned list are filtered by
     * {@code valid} and stored as an immutable copy, and which keeps {@code parser}'s view of the accepted
     * entries, rebuilt on every assignment and reset, for lists read far more often than set. Rejected entries
     * are omitted; reporting them is the job of whichever config reader supplied the raw list.
     */
    public static <D> Parsed<D> parsedStrings(String section, String key, List<String> defaultValue,
                                              Predicate<String> valid, Function<List<String>, D> parser,
                                              String comment) {
        return new Parsed<>(section, key, defaultValue, valid, parser, comment);
    }

    /** Restore the (already normalized) factory default. */
    public void reset() {
        value = defaultValue;
    }

    /** The name of the table this setting's key lives in. */
    public String section() {
        return section;
    }

    /** Assign a value after applying the normalization established by the factory method. */
    public void set(T value) {
        this.value = normalize.apply(value);
    }

    /** A boolean setting. */
    public static final class Flag extends ConfigValue<Boolean> {

        private Flag(String section, String key, boolean defaultValue, String comment) {
            super(section, key, defaultValue, UnaryOperator.identity(), comment);
        }
    }

    /** A string-list setting with a derived view; see {@link #parsedStrings}. */
    public static final class Parsed<D> extends ConfigValue<List<String>> {

        private final Function<List<String>, D> parser;
        private volatile D parsed;
        private final Predicate<String> valid;

        private Parsed(String section, String key, List<String> defaultValue, Predicate<String> valid,
                       Function<List<String>, D> parser, String comment) {
            super(section, key, defaultValue, filter(valid), comment);
            this.parser = parser;
            this.parsed = parser.apply(get());
            this.valid = valid;
        }

        /** The derived view of the current value. */
        public D parsed() {
            return parsed;
        }

        @Override
        public void reset() {
            super.reset();
            parsed = parser.apply(get());
        }

        @Override
        public void set(List<String> value) {
            super.set(value);
            parsed = parser.apply(get());
        }

        /** The entry guard. */
        public Predicate<String> valid() {
            return valid;
        }
    }

    /** An integer setting clamped to {@code [min, max]}. */
    public static final class Ranged extends ConfigValue<Integer> {

        private final int max;
        private final int min;

        private Ranged(String section, String key, int defaultValue, int min, int max, String comment) {
            super(section, key, defaultValue, value -> Math.max(min, Math.min(max, value)), comment);
            this.max = max;
            this.min = min;
        }

        public int max() {
            return max;
        }

        public int min() {
            return min;
        }
    }
}
