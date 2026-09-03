package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

/** Fabric command routing for the shared picker tree and its colliding server-side admin sibling. */
public final class FabricClientCommands {

    private FabricClientCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                CommandBuildContext context) {
        GooglyClientCommands.register(dispatcher, context);

        // Fabric executes a matching client root before consulting the server. Keep the
        // server-owned /sg admin path reachable by forwarding that disjoint subtree explicitly.
        LiteralArgumentBuilder<FabricClientCommandSource> admin =
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("admin")
                        .executes(FabricClientCommands::fallThroughToServer);
        admin.then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                        "arguments", StringArgumentType.greedyString())
                .executes(FabricClientCommands::fallThroughToServer));
        dispatcher.getRoot().getChild("sg").addChild(admin.build());
    }

    /** Add picker nodes to Minecraft's server-supplied dispatcher for completion and help display. */
    public static <S> void mergeSuggestions(CommandDispatcher<S> dispatcher) {
        GooglyClientCommands.register(dispatcher);
    }

    private static int fallThroughToServer(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        // Fabric forwards client commands only when its local dispatcher reports an unknown
        // command or a parse failure. This deliberate parse failure preserves the server-owned path.
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                .create(context.getInput());
    }
}
