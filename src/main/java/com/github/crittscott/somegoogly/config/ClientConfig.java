package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DISABLE_GOOGLY_EYES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES;
    private static final Set<String> loggedBadDisabledEntityEntries = new HashSet<>();

    // Keystone B: per-behavior toggles for the client-side eye expressions.
    public static final ForgeConfigSpec.BooleanValue EXPRESSION_BLINK;
    public static final ForgeConfigSpec.BooleanValue EXPRESSION_STARE;
    public static final ForgeConfigSpec.BooleanValue EXPRESSION_HURT_GROW;
    public static final ForgeConfigSpec.BooleanValue EXPRESSION_ANGER;
    public static final ForgeConfigSpec.BooleanValue EXPRESSION_SWIRL;

    static {
        BUILDER.push("Client Settings");

        DISABLE_GOOGLY_EYES = BUILDER
                .comment("Disable all googly eyes (client-side override)")
                .define("disableGooglyEyes", false);

        DISABLED_ENTITIES = BUILDER
                .comment("List of entities that should not get googly eyes (client-side override)")
                .defineList("disabledEntities", ArrayList::new, obj -> obj instanceof String);

        BUILDER.pop();

        BUILDER.push("Expressions");

        EXPRESSION_BLINK = BUILDER
                .comment("Eyes occasionally blink (and sometimes wink one eye)")
                .define("blink", true);
        EXPRESSION_STARE = BUILDER
                .comment("Eyes occasionally stop tracking and stare straight ahead")
                .define("stare", true);
        EXPRESSION_HURT_GROW = BUILDER
                .comment("Eyes briefly enlarge when the mob takes damage")
                .define("hurtGrow", true);
        EXPRESSION_ANGER = BUILDER
                .comment("Eyes tint red when the mob is angry (only mobs whose aggression is visible client-side)")
                .define("anger", true);
        EXPRESSION_SWIRL = BUILDER
                .comment("Villager pupils swirl when the villager levels up")
                .define("swirl", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static Set<ResourceLocation> disabledEntityIds() {
        Set<ResourceLocation> parsed = new LinkedHashSet<>();
        for (String entry : DISABLED_ENTITIES.get()) {
            ResourceLocation id = parseDisabledEntityId(entry);
            if (id != null) {
                parsed.add(id);
            }
        }
        return parsed;
    }

    public static boolean isEntityDisabled(ResourceLocation entityType) {
        return disabledEntityIds().contains(entityType);
    }

    private static ResourceLocation parseDisabledEntityId(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            logBadDisabledEntityEntry(entry);
            return null;
        }

        try {
            return new ResourceLocation(entry.trim());
        } catch (Exception e) {
            logBadDisabledEntityEntry(entry);
            return null;
        }
    }

    private static void logBadDisabledEntityEntry(String entry) {
        String key = String.valueOf(entry);
        if (loggedBadDisabledEntityEntries.add(key)) {
            SomeGoogly.LOGGER.error("Dropping invalid client disabledEntities entry '{}'; expected an entity id like 'minecraft:zombie'", key);
        }
    }
}
