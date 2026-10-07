package com.github.crittscott.somegoogly.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Mutable, validated runtime view of a loader-owned configuration value. */
public class ConfigValue<T> {

    private final T defaultValue;
    private final UnaryOperator<T> normalize;
    private volatile T value;

    private ConfigValue(T defaultValue, UnaryOperator<T> normalize) {
        this.normalize = normalize;
        this.defaultValue = normalize.apply(defaultValue);
        this.value = this.defaultValue;
    }

    /** Create a boolean value; no normalization is applied. */
    public static ConfigValue<Boolean> bool(boolean defaultValue) {
        return new ConfigValue<>(defaultValue, value -> value);
    }

    /**
     * Create an integer value whose default and every subsequently assigned value are clamped to
     * {@code [min, max]}.
     */
    public static ConfigValue<Integer> integer(int defaultValue, int min, int max) {
        return new ConfigValue<>(defaultValue, value -> Math.max(min, Math.min(max, value)));
    }

    /**
     * Create a string-list value whose default and every subsequently assigned list are filtered by
     * {@code valid} and stored as an immutable copy. Rejected entries are omitted; reporting them is
     * the job of whichever config reader supplied the raw list.
     */
    public static ConfigValue<List<String>> strings(List<String> defaultValue, Predicate<String> valid) {
        return new ConfigValue<>(defaultValue, filter(valid));
    }

    /**
     * Create a string-list value like {@link #strings} that also keeps {@code parser}'s view of the
     * accepted entries, rebuilt on every assignment and reset, for lists read far more often than set.
     */
    public static <D> Parsed<D> parsedStrings(List<String> defaultValue, Predicate<String> valid,
                                              Function<List<String>, D> parser) {
        return new Parsed<>(defaultValue, valid, parser);
    }

    private static UnaryOperator<List<String>> filter(Predicate<String> valid) {
        return values -> {
            List<String> accepted = new ArrayList<>();
            for (String value : values) {
                if (value != null && valid.test(value)) {
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

    /** Restore the (already normalized) factory default. */
    public void reset() {
        value = defaultValue;
    }

    /** Assign a value after applying the normalization established by the factory method. */
    public void set(T value) {
        this.value = normalize.apply(value);
    }

    /** A string-list value with a derived view; see {@link #parsedStrings}. */
    public static final class Parsed<D> extends ConfigValue<List<String>> {

        private final Function<List<String>, D> parser;
        private volatile D parsed;

        private Parsed(List<String> defaultValue, Predicate<String> valid, Function<List<String>, D> parser) {
            super(defaultValue, filter(valid));
            this.parser = parser;
            this.parsed = parser.apply(get());
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
    }
}
