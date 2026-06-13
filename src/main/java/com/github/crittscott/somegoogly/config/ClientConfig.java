package com.github.crittscott.somegoogly.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DISABLE_GOOGLY_EYES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES;

    static {
        BUILDER.push("Client Settings");

        DISABLE_GOOGLY_EYES = BUILDER
                .comment("Disable all googly eyes (client-side override)")
                .define("disableGooglyEyes", false);

        DISABLED_ENTITIES = BUILDER
                .comment("List of entities that should not get googly eyes (client-side override)")
                .defineList("disabledEntities", ArrayList::new, obj -> obj instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}