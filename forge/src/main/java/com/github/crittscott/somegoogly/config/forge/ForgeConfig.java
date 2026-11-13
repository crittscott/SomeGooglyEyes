package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** Loader-local conversion of the shared setting declarations to native Forge config values. */
final class ForgeConfig {

    private ForgeConfig() {
    }

    /**
     * Declare {@code settings} on {@code builder}, section by section, returning the step that copies the loaded
     * file's values into them.
     */
    static Runnable define(ForgeConfigSpec.Builder builder, List<ConfigValue<?>> settings) {
        List<Runnable> copies = new ArrayList<>();
        String section = null;
        for (ConfigValue<?> setting : settings) {
            if (!setting.section().equals(section)) {
                if (section != null) {
                    builder.pop();
                }
                section = setting.section();
                builder.push(section);
            }
            builder.comment(setting.comment().split("\n"));
            if (setting instanceof ConfigValue.Flag flag) {
                ForgeConfigSpec.BooleanValue spec = builder.define(flag.key(), flag.defaultValue().booleanValue());
                copies.add(() -> flag.set(spec.get()));
            } else if (setting instanceof ConfigValue.Ranged ranged) {
                ForgeConfigSpec.IntValue spec = builder.defineInRange(ranged.key(),
                        ranged.defaultValue().intValue(), ranged.min(), ranged.max());
                copies.add(() -> ranged.set(spec.get()));
            } else if (setting instanceof ConfigValue.Parsed<?> list) {
                ForgeConfigSpec.ConfigValue<List<? extends String>> spec = builder.defineList(list.key(),
                        list.defaultValue(), value -> value instanceof String string && list.valid().test(string));
                copies.add(() -> list.set(new ArrayList<>(spec.get())));
            }
        }
        builder.pop();
        return () -> copies.forEach(Runnable::run);
    }
}
