package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.FabricClientConfig;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entry point for configuration, networking, rendering, input, HUD, and commands.
 */
public final class SomeGooglyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.register();
        ClientCommandRegistrationEvent.EVENT.register(GooglyClientCommands::register);
        FabricClientConfig.load();
        FabricClientEvents.register();
        SomeGooglyCommon.LOGGER.info("{} client initializing on Fabric", SomeGooglyCommon.MOD_NAME);
    }
}
