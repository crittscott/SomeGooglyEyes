package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** NeoForge TOML storage for the shared client rendering preferences. */
public final class NeoForgeClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue DISABLE_GOOGLY_EYES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITIES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_MODS;
    private static final ModConfigSpec SPEC;

    static {
        BUILDER.push("Client Settings");
        DISABLE_GOOGLY_EYES = BUILDER.comment("Disable display of all googly eyes on this client.")
                .define(ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT);
        DISABLED_ENTITIES = BUILDER.comment("Entity ids that should not display googly eyes")
                .defineList(ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_ENTITIES_DEFAULT,
                        () -> "", value -> value instanceof String);
        DISABLED_MODS = BUILDER.comment("Mod namespaces whose entities should not display googly eyes")
                .defineList(ClientConfig.DISABLED_MODS_KEY, ClientConfig.DISABLED_MODS_DEFAULT,
                        () -> "", value -> value instanceof String);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private NeoForgeClientConfig() {
    }

    public static void register(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC,
                SomeGooglyCommon.MOD_ID + "-client.toml");
        modBus.addListener(NeoForgeClientConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading || event.getConfig().getSpec() != SPEC) {
            return;
        }
        ClientConfig.DISABLE_GOOGLY_EYES.set(DISABLE_GOOGLY_EYES.get());
        ClientConfig.DISABLED_ENTITIES.set(new ArrayList<>(DISABLED_ENTITIES.get()));
        ClientConfig.DISABLED_MODS.set(new ArrayList<>(DISABLED_MODS.get()));
        ClientConfig.invalidateCaches();
    }
}
