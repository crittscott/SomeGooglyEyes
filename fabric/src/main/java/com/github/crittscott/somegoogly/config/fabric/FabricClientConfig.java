package com.github.crittscott.somegoogly.config.fabric;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

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
            CommentedConfig document = TomlConfig.readOrCreate(path, render());
            Map<String, Object> values = TomlConfig.section(document, ClientConfig.SECTION);
            ClientConfig.resetDefaults();
            ClientConfig.DISABLE_GOOGLY_EYES.set(TomlConfig.bool(values,
                    ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT));
            TomlConfig.assign(ClientConfig.DISABLED_ENTITIES, ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.strings(values,
                    ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_ENTITIES_DEFAULT));
            TomlConfig.assign(ClientConfig.DISABLED_MODS, ClientConfig.DISABLED_MODS_KEY, TomlConfig.strings(values,
                    ClientConfig.DISABLED_MODS_KEY, ClientConfig.DISABLED_MODS_DEFAULT));
            TomlConfig.completeIfMissing(path, document, render());
        } catch (IOException | IllegalArgumentException e) {
            SomeGooglyCommon.LOGGER.error("Could not load Fabric client config {}", path, e);
        }
    }

    /** The commented file for the current values. */
    private static String render() {
        return """
                %s
                # %s
                %s = %s
                # %s
                %s = %s
                # %s
                %s = %s
                """.formatted(
                TomlConfig.table(ClientConfig.SECTION),
                ClientConfig.DISABLE_GOOGLY_EYES_COMMENT,
                ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES.defaultValue(),
                ClientConfig.DISABLED_ENTITIES_COMMENT,
                ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.stringList(ClientConfig.DISABLED_ENTITIES.defaultValue()),
                ClientConfig.DISABLED_MODS_COMMENT,
                ClientConfig.DISABLED_MODS_KEY, TomlConfig.stringList(ClientConfig.DISABLED_MODS.defaultValue()));
    }
}
