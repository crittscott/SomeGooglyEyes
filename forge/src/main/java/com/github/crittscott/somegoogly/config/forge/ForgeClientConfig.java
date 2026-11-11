package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ClientConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge TOML storage for the shared {@link ClientConfig} runtime values. */
public final class ForgeClientConfig {

    /** Copies the loaded file's values into {@link ClientConfig}. */
    private static final Runnable COPY_VALUES;
    private static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COPY_VALUES = ForgeConfig.define(builder, ClientConfig.settings());
        SPEC = builder.build();
    }

    private ForgeClientConfig() {
    }

    /** Register the CLIENT config spec and copy its values into {@code ClientConfig} on load and reload. */
    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, SPEC, ClientConfig.FILE_NAME);
        BusGroup modBusGroup = context.getModBusGroup();
        ModConfigEvent.Loading.getBus(modBusGroup).addListener(ForgeClientConfig::onConfigChanged);
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(ForgeClientConfig::onConfigChanged);
        ModConfigEvent.Unloading.getBus(modBusGroup).addListener(ForgeClientConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading || event.getConfig().getSpec() != SPEC) {
            return;
        }
        COPY_VALUES.run();
        ClientConfig.onApplied();
    }
}
