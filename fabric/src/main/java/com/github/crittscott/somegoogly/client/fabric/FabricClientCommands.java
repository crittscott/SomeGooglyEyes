package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** Fabric command routing for the local picker tree and server-owned {@code /sg} branches. */
public final class FabricClientCommands {

    private FabricClientCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        GooglyClientCommands.register(dispatcher);

        // Fabric executes a matching client root before consulting the server. Explicit parse
        // failures forward the disjoint world-mutation branches to the server dispatcher.
        addServerPath(dispatcher, "admin");
        addServerPath(dispatcher, "spawn");
        addServerPath(dispatcher, "spawnall");
        addServerPath(dispatcher, "mob");
    }

    /** Add picker nodes to Minecraft's server-supplied dispatcher for completion and help display. */
    public static <S> void mergeSuggestions(CommandDispatcher<S> dispatcher) {
        GooglyClientCommands.register(dispatcher);
    }

    private static void addServerPath(
            CommandDispatcher<FabricClientCommandSource> dispatcher, String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> node =
                LiteralArgumentBuilder.<FabricClientCommandSource>literal(name)
                        .executes(FabricClientCommands::fallThroughToServer);
        node.then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                        "arguments", StringArgumentType.greedyString())
                .executes(FabricClientCommands::fallThroughToServer));
        dispatcher.getRoot().getChild("sg").addChild(node.build());
    }

    private static int fallThroughToServer(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        // Fabric forwards client commands only when its local dispatcher reports an unknown
        // command or a parse failure. This deliberate parse failure preserves the server-owned path.
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                .create(context.getInput());
    }
}
