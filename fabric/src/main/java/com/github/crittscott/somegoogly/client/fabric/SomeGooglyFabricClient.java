package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.config.fabric.FabricClientConfig;
import com.github.crittscott.somegoogly.network.fabric.FabricClientNetworkTransport;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * Fabric client entry point for configuration, networking, rendering, input, HUD, and commands.
 */
public final class SomeGooglyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.register();
        FabricClientNetworkTransport.register();
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, context) -> FabricClientCommands.register(dispatcher, context));
        FabricClientConfig.load();
        FabricClientEvents.register();
        SomeGooglyCommon.LOGGER.info("{} client initialized on Fabric", SomeGooglyCommon.MOD_NAME);
    }
}
