package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ClientConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

/** Forge TOML storage for the shared {@link ClientConfig} runtime values. */
public final class ForgeClientConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue DISABLE_GOOGLY_EYES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_MODS;
    private static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("Client Settings");
        DISABLE_GOOGLY_EYES = BUILDER.comment("Disable display of all googly eyes on this client.")
                .define(ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT);
        DISABLED_ENTITIES = BUILDER.comment("Entity ids that should not display googly eyes")
                .defineList(ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_ENTITIES_DEFAULT,
                        value -> value instanceof String);
        DISABLED_MODS = BUILDER.comment("Mod namespaces whose entities should not display googly eyes")
                .defineList(ClientConfig.DISABLED_MODS_KEY, ClientConfig.DISABLED_MODS_DEFAULT,
                        value -> value instanceof String);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ForgeClientConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeClientConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        ClientConfig.DISABLE_GOOGLY_EYES.set(DISABLE_GOOGLY_EYES.get());
        ClientConfig.DISABLED_ENTITIES.set(new ArrayList<>(DISABLED_ENTITIES.get()));
        ClientConfig.DISABLED_MODS.set(new ArrayList<>(DISABLED_MODS.get()));
        ClientConfig.invalidateCaches();
    }
}
