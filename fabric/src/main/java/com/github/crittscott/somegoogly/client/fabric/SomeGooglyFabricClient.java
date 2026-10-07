package com.github.crittscott.somegoogly.client.fabric;

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
        FabricClientNetworkTransport.register();
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, context) -> FabricClientCommands.register(dispatcher));
        FabricClientConfig.load();
        FabricClientEvents.register();
    }
}
