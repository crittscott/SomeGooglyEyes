package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads Fabric's client TOML into the shared runtime configuration. Like Forge, a file missing any key, such as
 * one from an earlier release, is rewritten with the values it holds and each missing key at its default.
 */
public final class FabricClientConfig {

    private static final List<String> KEYS = List.of(ClientConfig.DISABLE_GOOGLY_EYES_KEY,
            ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_MODS_KEY);

    private FabricClientConfig() {
    }

    /** Read {@code config/somegoogly-client.toml}, writing it with defaults if absent, into {@code ClientConfig}. */
    public static void load() {
        ClientConfig.resetDefaults();
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ClientConfig.FILE_NAME);
        try {
            Map<String, Object> values = TomlConfig.readOrCreate(path, render());
            ClientConfig.DISABLE_GOOGLY_EYES.set(TomlConfig.bool(values,
                    ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES_DEFAULT));
            TomlConfig.assign(ClientConfig.DISABLED_ENTITIES, ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.strings(values,
                    ClientConfig.DISABLED_ENTITIES_KEY, ClientConfig.DISABLED_ENTITIES_DEFAULT));
            TomlConfig.assign(ClientConfig.DISABLED_MODS, ClientConfig.DISABLED_MODS_KEY, TomlConfig.strings(values,
                    ClientConfig.DISABLED_MODS_KEY, ClientConfig.DISABLED_MODS_DEFAULT));
            TomlConfig.completeIfMissing(path, values, KEYS, render());
        } catch (IOException e) {
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
                ClientConfig.DISABLE_GOOGLY_EYES_KEY, ClientConfig.DISABLE_GOOGLY_EYES.get(),
                ClientConfig.DISABLED_ENTITIES_COMMENT,
                ClientConfig.DISABLED_ENTITIES_KEY, TomlConfig.stringList(ClientConfig.DISABLED_ENTITIES.get()),
                ClientConfig.DISABLED_MODS_COMMENT,
                ClientConfig.DISABLED_MODS_KEY, TomlConfig.stringList(ClientConfig.DISABLED_MODS.get()));
    }
}
