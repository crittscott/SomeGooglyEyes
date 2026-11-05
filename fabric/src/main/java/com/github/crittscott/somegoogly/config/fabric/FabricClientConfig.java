package com.github.crittscott.somegoogly.config.fabric;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads Fabric's client TOML into the shared runtime configuration. Like Forge, a file missing any key, such as
 * one from an earlier release, is completed with absent keys at their defaults, retaining its values and comments.
 */
public final class FabricClientConfig {

    private FabricClientConfig() {
    }

    /** Read {@code config/somegoogly-client.toml}, writing it with defaults if absent, into {@code ClientConfig}. */
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ClientConfig.FILE_NAME);
        try {
            String defaults = TomlConfig.render(ClientConfig.settings());
            CommentedConfig document = TomlConfig.readOrCreate(path, defaults);
            TomlConfig.apply(document, ClientConfig.settings());
            TomlConfig.completeIfMissing(path, document, defaults);
        } catch (IOException | IllegalArgumentException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric client config {}", path, e);
        }
    }
}
