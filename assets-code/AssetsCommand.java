package com.github.crittscott.assets;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AssetsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("assets")
                .executes(AssetsCommand::dumpResources)
                .then(Commands.literal("skeleton").executes(ctx -> dumpEntities(ctx, Mode.SKELETON)))
                .then(Commands.literal("geometry").executes(ctx -> dumpEntities(ctx, Mode.GEOMETRY)))
                .then(Commands.literal("head").executes(ctx -> dumpEntities(ctx, Mode.HEAD)))
                .then(Commands.literal("blockbench").executes(ctx -> dumpEntities(ctx, Mode.BLOCKBENCH)))
        );
    }

    /** Bare {@code /assets}: dump the static resource and data-pack assets. */
    private static int dumpResources(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> Component.literal("Asset extraction started"), false);
            AssetDumper.dumpAll(context.getSource());
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Asset extraction failed: " + e.getMessage()));
            AssetsMod.getLogger().error("Asset extraction failed", e);
            return 0;
        }
    }

    /** {@code /assets <mode>}: extract entity models in the given representation. */
    private static int dumpEntities(CommandContext<CommandSourceStack> context, Mode mode) {
        try {
            context.getSource().sendSuccess(
                    () -> Component.literal("Entity " + mode.folder + " extraction started"), false);
            EntityModelDumper.dump(context.getSource(), mode);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("Entity extraction failed: " + e.getMessage()));
            AssetsMod.getLogger().error("Entity extraction failed", e);
            return 0;
        }
    }
}
