package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** Loads Fabric's client TOML into the shared runtime configuration. */
public final class FabricClientConfig {

    private static final String DEFAULTS = """
            [Client Settings]
            disableGooglyEyes = false
            disabledEntities = []
            disabledMods = []
            """;

    private FabricClientConfig() {
    }

    public static void load() {
        ClientConfig.resetDefaults();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("somegoogly-client.toml");
        try {
            Map<String, Object> values = FabricToml.readOrCreate(path, DEFAULTS);
            ClientConfig.DISABLE_GOOGLY_EYES.set(FabricToml.bool(values, "disableGooglyEyes", false));
            ClientConfig.DISABLED_ENTITIES.set(FabricToml.strings(values, "disabledEntities"));
            ClientConfig.DISABLED_MODS.set(FabricToml.strings(values, "disabledMods"));
            ClientConfig.invalidateCaches();
        } catch (IOException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric client config {}", path, e);
        }
    }
}
