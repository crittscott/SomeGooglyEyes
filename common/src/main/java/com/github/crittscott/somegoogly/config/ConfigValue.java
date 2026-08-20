package com.github.crittscott.somegoogly.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Mutable, validated runtime view of a loader-owned configuration value. */
public final class ConfigValue<T> {

    private final T defaultValue;
    private final UnaryOperator<T> normalize;
    private volatile T value;

    private ConfigValue(T defaultValue, UnaryOperator<T> normalize) {
        this.normalize = normalize;
        this.defaultValue = normalize.apply(defaultValue);
        this.value = this.defaultValue;
    }

    public static ConfigValue<Boolean> bool(boolean defaultValue) {
        return new ConfigValue<>(defaultValue, value -> value);
    }

    public static ConfigValue<Integer> integer(int defaultValue, int min, int max) {
        return new ConfigValue<>(defaultValue, value -> Math.max(min, Math.min(max, value)));
    }

    public static ConfigValue<List<String>> strings(List<String> defaultValue, Predicate<String> valid) {
        return new ConfigValue<>(defaultValue, values -> {
            List<String> accepted = new ArrayList<>();
            for (String value : values) {
                if (valid.test(value)) {
                    accepted.add(value);
                }
            }
            return List.copyOf(accepted);
        });
    }

    public T get() {
        return value;
    }

    public void reset() {
        value = defaultValue;
    }

    public void set(T value) {
        this.value = normalize.apply(value);
    }
}
