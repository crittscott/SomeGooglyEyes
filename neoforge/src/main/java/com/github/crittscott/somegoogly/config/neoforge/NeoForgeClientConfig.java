package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.config.ClientConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** NeoForge TOML storage for the shared client rendering preferences. */
public final class NeoForgeClientConfig {

    /** Copies the loaded file's values into {@link ClientConfig}. */
    private static final Runnable COPY_VALUES;
    private static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COPY_VALUES = NeoForgeConfig.define(builder, ClientConfig.settings());
        SPEC = builder.build();
    }

    private NeoForgeClientConfig() {
    }

    /** Register the CLIENT config spec and copy its values into {@code ClientConfig} on load and reload. */
    public static void register(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC, ClientConfig.FILE_NAME);
        modBus.addListener(NeoForgeClientConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading || event.getConfig().getSpec() != SPEC) {
            return;
        }
        COPY_VALUES.run();
        ClientConfig.onApplied();
    }
}
