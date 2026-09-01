package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.config.ServerConfigFile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Loads the active world's server TOML into the shared runtime configuration. */
public final class NeoForgeServerConfig {

    private NeoForgeServerConfig() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeServerConfig::onServerAboutToStart);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ServerConfigFile.load(event.getServer());
    }
}
