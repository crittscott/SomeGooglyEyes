package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ServerConfigFile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/** Loads the active world's server TOML into the shared runtime configuration. */
public final class ForgeServerConfig {

    private ForgeServerConfig() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ForgeServerConfig::onServerAboutToStart);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ServerConfigFile.load(event.getServer());
    }
}
