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
            [%s]
            # %s
            %s = %s
            # %s
            %s = %s
            # %s
            %s = %s
            """.formatted(
            ClientConfig.SECTION,
            ClientConfig.DISABLE_GOOGLY_EYES_COMMENT,
            ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT,
            ClientConfig.DISABLED_ENTITIES_COMMENT,
            ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.stringList(ClientConfig.DISABLED_ENTITIES_DEFAULT),
            ClientConfig.DISABLED_MODS_COMMENT,
            ClientConfig.DISABLED_MODS_KEY, TomlConfig.stringList(ClientConfig.DISABLED_MODS_DEFAULT));

    private FabricClientConfig() {
    }

    /** Read {@code config/somegoogly-client.toml}, writing it with defaults if absent, into {@code ClientConfig}. */
    public static void load() {
        ClientConfig.resetDefaults();
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ClientConfig.FILE_NAME);
        try {
            Map<String, Object> values = TomlConfig.readOrCreate(path, DEFAULTS);
            ClientConfig.DISABLE_GOOGLY_EYES.set(TomlConfig.bool(values,
                    ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT));
            TomlConfig.assign(ClientConfig.DISABLED_ENTITIES, ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.strings(values,
                    ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_ENTITIES_DEFAULT));
            TomlConfig.assign(ClientConfig.DISABLED_MODS, ClientConfig.DISABLED_MODS_KEY, TomlConfig.strings(values,
                    ClientConfig.DISABLED_MODS_KEY, ClientConfig.DISABLED_MODS_DEFAULT));
        } catch (IOException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric client config {}", path, e);
        }
    }
}
