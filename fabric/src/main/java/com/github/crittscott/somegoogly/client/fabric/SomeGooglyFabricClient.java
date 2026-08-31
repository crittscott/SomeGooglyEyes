package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.config.fabric.FabricClientConfig;
import com.github.crittscott.somegoogly.network.fabric.FabricClientNetworkTransport;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import java.util.stream.Collectors;

/**
 * Fabric client entry point for configuration, networking, rendering, input, HUD, and commands.
 */
public final class SomeGooglyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SomeGooglyCommon.LOGGER.debug("Fabric client debug: client entrypoint invoked");
        ClientNetworkHandler.register();
        FabricClientNetworkTransport.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            FabricClientCommands.register(dispatcher, context);
            CommandNode<?> sg = dispatcher.getRoot().getChild("sg");
            SomeGooglyCommon.LOGGER.debug(
                    "Fabric client debug: local /sg tree registered with children [{}]",
                    children(sg));
        });
        FabricClientConfig.load();
        FabricClientEvents.register();
        SomeGooglyCommon.LOGGER.info("{} client initializing on Fabric", SomeGooglyCommon.MOD_NAME);
    }

    private static String children(CommandNode<?> node) {
        if (node == null) {
            return "missing";
        }
        return node.getChildren().stream()
                .map(CommandNode::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
