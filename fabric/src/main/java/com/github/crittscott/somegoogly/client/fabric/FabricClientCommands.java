package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.client.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.command.GooglyServerCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;

/** Fabric command routing for the local picker tree and server-owned {@code /sg} branches. */
public final class FabricClientCommands {

    private FabricClientCommands() {
    }

    /** Register the client {@code /sg} tree and forward its server-owned branches to the server. */
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        GooglyClientCommands.register(dispatcher);

        // Fabric executes a matching client root before consulting the server. Explicit parse
        // failures forward the disjoint world-mutation branches to the server dispatcher.
        GooglyServerCommands.SERVER_BRANCHES.forEach(branch -> addServerPath(dispatcher, branch));
    }

    private static void addServerPath(
            CommandDispatcher<FabricClientCommandSource> dispatcher, String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> node =
                LiteralArgumentBuilder.<FabricClientCommandSource>literal(name)
                        .executes(FabricClientCommands::fallThroughToServer);
        node.then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                        "arguments", StringArgumentType.greedyString())
                .executes(FabricClientCommands::fallThroughToServer));
        dispatcher.getRoot().getChild(GooglyServerCommands.ROOT).addChild(node.build());
    }

    /**
     * Add the picker verbs to the client's copy of the server command tree, which drives chat parsing and
     * completion. Fabric API copies each client node into that tree before copying the node's children, so when
     * the server already sends {@code /sg} the server's node wins the name merge and the picker verbs land on
     * a discarded copy. A tree built whole before it is added keeps every verb through Brigadier's merge.
     */
    public static void addPickerToServerTree(CommandDispatcher<SharedSuggestionProvider> serverTree) {
        CommandDispatcher<SharedSuggestionProvider> picker = new CommandDispatcher<>();
        GooglyClientCommands.register(picker);
        serverTree.getRoot().addChild(picker.getRoot().getChild(GooglyServerCommands.ROOT));
    }

    private static int fallThroughToServer(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        // Fabric forwards client commands only when its local dispatcher reports an unknown
        // command or a parse failure. This deliberate parse failure preserves the server-owned path.
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                .create(context.getInput());
    }
}
