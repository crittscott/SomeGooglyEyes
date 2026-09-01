package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.config.ServerConfigFile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Loads the active world's server TOML into the shared runtime configuration. */
public final class FabricServerConfig {

    private FabricServerConfig() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerConfigFile::load);
    }
}
