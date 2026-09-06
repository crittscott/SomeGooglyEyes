package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerConfigFile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Loads NeoForge server settings from the active world's serverconfig directory. */
public final class NeoForgeServerConfig {

    private NeoForgeServerConfig() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeServerConfig::onServerStarting);
        gameBus.addListener(NeoForgeServerConfig::onServerStopped);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        ServerConfigFile.load(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerConfig.resetDefaults();
    }
}
