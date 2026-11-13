package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads Fabric's client TOML into the shared runtime configuration. A file missing any key, such as one from an
 * earlier release, is completed with the absent keys at their defaults, retaining its values and comments.
 */
public final class FabricClientConfig {

    private FabricClientConfig() {
    }

    /** Read {@code config/somegoogly-client.toml}, writing it with defaults if absent, into {@code ClientConfig}. */
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ClientConfig.FILE_NAME);
        try {
            TomlConfig.load(path, ClientConfig.settings());
            ClientConfig.onApplied();
        } catch (IOException | IllegalArgumentException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric client config {}; keeping the values already in effect",
                    path, e);
        }
    }
}
