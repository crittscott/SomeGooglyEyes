package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.TomlConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** Loads Fabric's client TOML into the shared runtime configuration. */
public final class FabricClientConfig {

    private static final String DEFAULTS = """
            [Client Settings]
            %s = %s
            %s = %s
            %s = %s
            """.formatted(
            ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT,
            ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.stringList(ClientConfig.DISABLED_ENTITIES_DEFAULT),
            ClientConfig.DISABLED_MODS_KEY, TomlConfig.stringList(ClientConfig.DISABLED_MODS_DEFAULT));

    private FabricClientConfig() {
    }

    public static void load() {
        ClientConfig.resetDefaults();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("somegoogly-client.toml");
        try {
            Map<String, Object> values = TomlConfig.readOrCreate(path, DEFAULTS);
            ClientConfig.DISABLE_GOOGLY_EYES.set(TomlConfig.bool(values,
                    ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT));
            ClientConfig.DISABLED_ENTITIES.set(TomlConfig.strings(values,
                    ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_ENTITIES_DEFAULT));
            ClientConfig.DISABLED_MODS.set(TomlConfig.strings(values,
                    ClientConfig.DISABLED_MODS_KEY, ClientConfig.DISABLED_MODS_DEFAULT));
            ClientConfig.invalidateCaches();
        } catch (IOException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric client config {}", path, e);
        }
    }
}
