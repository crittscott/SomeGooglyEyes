package com.github.crittscott.somegoogly.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue GOOGLY_EYES_ENABLED;
    public static final ForgeConfigSpec.IntValue GLOBAL_PERCENT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_OVERRIDES;

    public static HashMap<ResourceLocation, Integer> entityOverrideParsed = new HashMap<>();

    static {
        BUILDER.push("Server Settings");

        GOOGLY_EYES_ENABLED = BUILDER
                .comment("Enable googly eyes globally")
                .define("googlyEyesEnabled", true);

        GLOBAL_PERCENT = BUILDER
                .comment("Global percentage chance for entities to get googly eyes")
                .defineInRange("globalPercent", 99, 0, 100);

        ENTITY_OVERRIDES = BUILDER
                .comment("Entity-specific override percentages in format 'modid:entity,percent'")
                .defineList("entityOverrides", ArrayList::new, ServerConfig::validateOverride);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    private static boolean validateOverride(Object o) {
        if (o instanceof String) {
            String[] split = ((String) o).split(",");
            if (split.length == 2) {
                try {
                    new ResourceLocation(split[0]);
                    int percent = Integer.parseInt(split[1]);
                    return percent >= 0 && percent <= 100;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public static void parseOverrides() {
        entityOverrideParsed.clear();
        for (String s : ENTITY_OVERRIDES.get()) {
            String[] split = s.split(",");
            entityOverrideParsed.put(new ResourceLocation(split[0]), Integer.parseInt(split[1]));
        }
    }
}