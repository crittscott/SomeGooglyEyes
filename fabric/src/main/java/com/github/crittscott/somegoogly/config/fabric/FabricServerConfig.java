package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/**
 * Creates and loads the active world's server TOML when the server starts and again at the start of
 * each {@code /reload}, since Fabric has no config system; values return to defaults when the server stops.
 * {@link ServerConfig#settings} drives the commented file and the load. A file missing any key, such as one from an
 * earlier release, is completed with only the absent keys at their defaults; existing values and comments are
 * retained.
 */
public final class FabricServerConfig {

    /** The per-world directory Forge and NeoForge keep server configs in, mirrored here. */
    private static final String SERVER_CONFIG_DIR = "serverconfig";

    private FabricServerConfig() {
    }

    /** Load the server config at server start and each {@code /reload}, and reset it at server stop. */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerConfig::load);
        // Before the reload's eye-config broadcast, so clients receive the re-read master switch.
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resources) -> load(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerConfig.resetDefaults());
    }

    private static void load(MinecraftServer server) {
        if (load(server.getWorldPath(LevelResource.ROOT).resolve(SERVER_CONFIG_DIR).resolve(ServerConfig.FILE_NAME))) {
            ServerServices.onServerConfigApplied();
        }
    }

    /**
     * Load the server config at {@code path} into {@link ServerConfig}, writing the defaults there if it is absent
     * and adding any keys it lacks. Returns whether it loaded; a failure is logged.
     */
    public static boolean load(Path path) {
        return TomlConfig.load(path, ServerConfig.settings());
    }
}
