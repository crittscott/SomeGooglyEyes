package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.client.picker.PickerExporter;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.picker.PickerState.ListedEye;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The {@code /sg} command tree — the in-game eye-placement CLI. Registered as <b>client</b> commands
 * (via {@link RegisterClientCommandsEvent}) because the editing they drive lives entirely in the
 * client-side {@link PickerState}. Like the keyboard picker, they require <b>creative mode</b>.
 *
 * <p>Each verb is a single full literal; there are no short aliases. The picker keyboard and this CLI
 * call the same {@link PickerState} methods, so they stay in lock-step.
 */
public class GooglyClientCommands {

    private static final DynamicCommandExceptionType BAD_INDEX =
            new DynamicCommandExceptionType(n -> Component.literal("No eye #" + n + "."));
    private static final DynamicCommandExceptionType BAD_PART =
            new DynamicCommandExceptionType(t -> Component.literal("No such part: " + t));
    private static final DynamicCommandExceptionType BAD_VARIANT =
            new DynamicCommandExceptionType(n -> Component.literal("No variant #" + n + "."));
    private static final SimpleCommandExceptionType CANT_DELETE_LAST_VARIANT =
            new SimpleCommandExceptionType(Component.literal("Can't delete the last variant — there's always at least one."));
    private static final SimpleCommandExceptionType NO_PART =
            new SimpleCommandExceptionType(Component.literal("Pick a part first (/sg part <name>)."));
    private static final SimpleCommandExceptionType NOT_CHOSEN =
            new SimpleCommandExceptionType(Component.literal("Choose a mob first (/sg choose)."));
    private static final SimpleCommandExceptionType NOT_CREATIVE =
            new SimpleCommandExceptionType(Component.literal("The picker requires creative mode."));
    private static final SimpleCommandExceptionType SINGLEPLAYER_ONLY =
            new SimpleCommandExceptionType(Component.literal("Spawn-all only works in single-player."));

    private static double azi(EyeDraft e) {
        return e.azimuth != null ? e.azimuth : HeadInfo.DEFAULT_AZIMUTH;
    }

    private static int choose(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        PickerState.active = true; // turn the picker on so the preview/gizmo render
        feedback(ctx, PickerState.lockOn());
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        double x = FloatArgumentType.getFloat(ctx, "x");
        double y = FloatArgumentType.getFloat(ctx, "y");
        double z = FloatArgumentType.getFloat(ctx, "z");
        PickerState.createEye(x, y, z);
        feedback(ctx, String.format("Created eye at [%.3f, %.3f, %.3f].", x, y, z));
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.delete(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "Deleted eye #" + n + " (" + PickerState.currentEyeCount() + " left).");
        return 1;
    }

    private static int export(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        feedback(ctx, PickerExporter.export());
        return 1;
    }

    private static int exportAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        feedback(ctx, PickerExporter.exportAll());
        return 1;
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal("[Googly] " + text), false);
    }

    private static double incl(EyeDraft e) {
        return e.inclination != null ? e.inclination : HeadInfo.DEFAULT_INCLINATION;
    }

    private static int listEyes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        List<ListedEye> list = PickerState.currentEyes();
        feedback(ctx, "Eyes in variant #" + (PickerState.variantIndex + 1) + " (" + list.size() + "):");
        for (int i = 0; i < list.size(); i++) {
            ListedEye le = list.get(i);
            EyeDraft e = le.eye;
            String mark = i == PickerState.selectedIndex ? " *" : "";
            feedback(ctx, String.format("  %d. part=%s pos[%.3f, %.3f, %.3f] incl %.0f azi %.0f%s",
                    i + 1, le.part, e.position[0], e.position[1], e.position[2], incl(e), azi(e), mark));
        }
        return 1;
    }

    private static int listParts(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        feedback(ctx, "Parts (" + PickerState.parts.size() + "):");
        for (int i = 0; i < PickerState.parts.size(); i++) {
            feedback(ctx, "  " + (i + 1) + ". " + PickerState.parts.get(i));
        }
        return 1;
    }

    private static int listVariants(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        feedback(ctx, "Variants (" + PickerState.variantCount() + "):");
        for (int i = 0; i < PickerState.variantCount(); i++) {
            PickerState.DraftVariant v = PickerState.variants.get(i);
            String mark = i == PickerState.variantIndex ? " *" : "";
            feedback(ctx, String.format("  %d. weight %.2f, %d eyes%s", i + 1, v.weight, v.eyes.size(), mark));
        }
        return 1;
    }

    private static int move(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setPosition(opt(MaybeFloatArgumentType.get(ctx, "x")),
                opt(MaybeFloatArgumentType.get(ctx, "y")),
                opt(MaybeFloatArgumentType.get(ctx, "z")));
        EyeDraft e = PickerState.currentEye;
        feedback(ctx, String.format("Position [%.3f, %.3f, %.3f].", e.position[0], e.position[1], e.position[2]));
        return 1;
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static Double opt(Optional<Float> value) {
        return value.map(Float::doubleValue).orElse(null);
    }

    private static int part(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        String target = StringArgumentType.getString(ctx, "target");
        if (target.equalsIgnoreCase("none")) {
            PickerState.clearPart();
            feedback(ctx, "Part cleared (none).");
            return 1;
        }
        boolean ok;
        try {
            ok = PickerState.setPartByNumber(Integer.parseInt(target));
        } catch (NumberFormatException notANumber) {
            ok = PickerState.setPartByName(target);
        }
        if (!ok) {
            throw BAD_PART.create(target);
        }
        feedback(ctx, "Part: " + PickerState.currentPart);
        return 1;
    }

    private static int posrot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setPosition(opt(MaybeFloatArgumentType.get(ctx, "x")),
                opt(MaybeFloatArgumentType.get(ctx, "y")),
                opt(MaybeFloatArgumentType.get(ctx, "z")));
        PickerState.setRotation(opt(MaybeFloatArgumentType.get(ctx, "inclination")),
                opt(MaybeFloatArgumentType.get(ctx, "azimuth")));
        EyeDraft e = PickerState.currentEye;
        feedback(ctx, String.format("Position [%.3f, %.3f, %.3f], rotation incl %.0f° azi %.0f°.",
                e.position[0], e.position[1], e.position[2], incl(e), azi(e)));
        return 1;
    }

    private static <T> LiteralArgumentBuilder<CommandSourceStack> prop(String name, ArgumentType<T> type,
                                                                       Command<CommandSourceStack> exec) {
        RequiredArgumentBuilder<CommandSourceStack, T> v = Commands.argument("v", type);
        terminal(v, exec);
        return Commands.literal(name).then(v);
    }

    private static int propCorneaColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setCorneaColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "corneaColors set.");
        return 1;
    }

    private static int propEyeScale(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setEyeScale(v);
        feedback(ctx, "eyeScale = " + v);
        return 1;
    }

    private static int propGlow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        boolean v = BoolArgumentType.getBool(ctx, "v");
        PickerState.setGlow(v);
        feedback(ctx, "glow = " + v);
        return 1;
    }

    private static int propInvis(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        boolean v = BoolArgumentType.getBool(ctx, "v");
        PickerState.setInvis(v);
        feedback(ctx, "affectedByInvisibility = " + v);
        return 1;
    }

    private static int propIrisColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setIrisColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "irisColors set.");
        return 1;
    }

    private static int propIrisScale(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setIrisScale(v);
        feedback(ctx, "irisScale = " + v);
        return 1;
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> sg = Commands.literal("sg");

        verb(sg, "choose", b -> terminal(b, GooglyClientCommands::choose));
        verb(sg, "unchoose", b -> terminal(b, GooglyClientCommands::unchoose));

        verb(sg, "part", b -> {
            RequiredArgumentBuilder<CommandSourceStack, String> target =
                    Commands.argument("target", StringArgumentType.word());
            terminal(target, GooglyClientCommands::part);
            b.then(target);
        });

        verb(sg, "create", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Float> z =
                    Commands.argument("z", FloatArgumentType.floatArg());
            terminal(z, GooglyClientCommands::create);
            b.then(Commands.argument("x", FloatArgumentType.floatArg())
                    .then(Commands.argument("y", FloatArgumentType.floatArg()).then(z)));
        });

        // move: ~ supported per axis to leave an axis unchanged.
        verb(sg, "move", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Optional<Float>> z =
                    Commands.argument("z", MaybeFloatArgumentType.maybeFloat());
            terminal(z, GooglyClientCommands::move);
            b.then(Commands.argument("x", MaybeFloatArgumentType.maybeFloat())
                    .then(Commands.argument("y", MaybeFloatArgumentType.maybeFloat()).then(z)));
        });

        verb(sg, "rot", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Optional<Float>> azimuth =
                    Commands.argument("azimuth", MaybeFloatArgumentType.maybeFloat());
            terminal(azimuth, GooglyClientCommands::rot);
            b.then(Commands.argument("inclination", MaybeFloatArgumentType.maybeFloat()).then(azimuth));
        });

        // posrot: move + rot in one go (the common case). ~ supported per component to leave it unchanged.
        verb(sg, "posrot", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Optional<Float>> azimuth =
                    Commands.argument("azimuth", MaybeFloatArgumentType.maybeFloat());
            terminal(azimuth, GooglyClientCommands::posrot);
            b.then(Commands.argument("x", MaybeFloatArgumentType.maybeFloat())
                    .then(Commands.argument("y", MaybeFloatArgumentType.maybeFloat())
                            .then(Commands.argument("z", MaybeFloatArgumentType.maybeFloat())
                                    .then(Commands.argument("inclination", MaybeFloatArgumentType.maybeFloat())
                                            .then(azimuth)))));
        });

        verb(sg, "save", b -> terminal(b, GooglyClientCommands::save));

        verb(sg, "select", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
            terminal(n, GooglyClientCommands::select);
            b.then(n);
        });
        verb(sg, "delete", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
            terminal(n, GooglyClientCommands::delete);
            b.then(n);
        });

        // variant: new / <n> (switch) / del <n> / weight <w>. Literals resolve before the bare integer arg.
        verb(sg, "variant", b -> {
            verb(b, "new", x -> terminal(x, GooglyClientCommands::variantNew));
            verb(b, "weight", x -> {
                RequiredArgumentBuilder<CommandSourceStack, Float> w = Commands.argument("w", FloatArgumentType.floatArg(0));
                terminal(w, GooglyClientCommands::variantWeight);
                x.then(w);
            });
            verb(b, "del", x -> {
                RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
                terminal(n, GooglyClientCommands::variantDelete);
                x.then(n);
            });
            RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
            terminal(n, GooglyClientCommands::variantSelect);
            b.then(n);
        });

        verb(sg, "list", b -> {
            verb(b, "parts", x -> terminal(x, GooglyClientCommands::listParts));
            verb(b, "eyes", x -> terminal(x, GooglyClientCommands::listEyes));
            verb(b, "variants", x -> terminal(x, GooglyClientCommands::listVariants));
        });

        verb(sg, "properties", b -> {
            b.then(prop("eyescale", FloatArgumentType.floatArg(0), GooglyClientCommands::propEyeScale));
            b.then(prop("irisscale", FloatArgumentType.floatArg(0), GooglyClientCommands::propIrisScale));
            b.then(Commands.literal("corneacolor").then(rgb(GooglyClientCommands::propCorneaColor)));
            b.then(Commands.literal("iriscolor").then(rgb(GooglyClientCommands::propIrisColor)));
            b.then(prop("glow", BoolArgumentType.bool(), GooglyClientCommands::propGlow));
            b.then(prop("invis", BoolArgumentType.bool(), GooglyClientCommands::propInvis));
        });

        verb(sg, "export", b -> terminal(b, GooglyClientCommands::export));
        verb(sg, "exportall", b -> terminal(b, GooglyClientCommands::exportAll));

        // Behavior testing lives in the server-side /sg admin command (the schedule is server-owned).

        // spawnall [mod] — bare spawns every mod; an optional namespace narrows it (a debugging aid).
        verb(sg, "spawnall", b -> {
            terminal(b, GooglyClientCommands::spawnAll);
            RequiredArgumentBuilder<CommandSourceStack, String> mod =
                    Commands.argument("mod", StringArgumentType.word());
            terminal(mod, GooglyClientCommands::spawnAll);
            b.then(mod);
        });

        dispatcher.register(sg);
    }

    private static void requireChosen() throws CommandSyntaxException {
        if (!PickerState.active || PickerState.target() == null) {
            throw NOT_CHOSEN.create();
        }
    }

    private static void requireCreative() throws CommandSyntaxException {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isCreative()) {
            throw NOT_CREATIVE.create();
        }
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Float> rgb(Command<CommandSourceStack> exec) {
        RequiredArgumentBuilder<CommandSourceStack, Float> bArg = Commands.argument("b", FloatArgumentType.floatArg(0, 1));
        terminal(bArg, exec);
        return Commands.argument("r", FloatArgumentType.floatArg(0, 1))
                .then(Commands.argument("g", FloatArgumentType.floatArg(0, 1)).then(bArg));
    }

    private static int rot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setRotation(opt(MaybeFloatArgumentType.get(ctx, "inclination")),
                opt(MaybeFloatArgumentType.get(ctx, "azimuth")));
        EyeDraft e = PickerState.currentEye;
        feedback(ctx, String.format("Rotation incl %.0f° azi %.0f°.", incl(e), azi(e)));
        return 1;
    }

    private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        if (!PickerState.save()) {
            throw NO_PART.create();
        }
        feedback(ctx, "Saved eye #" + (PickerState.selectedIndex + 1) + " (variant "
                + (PickerState.variantIndex + 1) + ", " + PickerState.currentEyeCount() + " eyes).");
        return 1;
    }

    private static int select(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.select(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "Editing eye #" + n + ".");
        return 1;
    }

    private static int spawnAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            throw SINGLEPLAYER_ONLY.create();
        }
        // The mod-namespace filter is optional; absent when the bare `spawnall` node executes.
        String mod;
        try {
            mod = StringArgumentType.getString(ctx, "mod");
        } catch (IllegalArgumentException noArg) {
            mod = null;
        }
        // Spawning is server-side work; resolve the server-side player on the server thread and run there.
        UUID uuid = mc.player.getUUID();
        String modFilter = mod;
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                SpawnAllCommand.spawn(player, modFilter);
            }
        });
        feedback(ctx, mod == null ? "Spawning mobs…" : "Spawning " + mod + " mobs…");
        return 1;
    }

    /** Mark a node executable: running it does this verb's own work. */
    private static void terminal(ArgumentBuilder<CommandSourceStack, ?> node, Command<CommandSourceStack> leaf) {
        node.executes(leaf);
    }

    private static int unchoose(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        PickerState.unlock();
        feedback(ctx, "Selection cleared.");
        return 1;
    }

    private static int variantDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (n > PickerState.variantCount()) {
            throw BAD_VARIANT.create(n);
        }
        if (!PickerState.deleteVariant(n)) {
            throw CANT_DELETE_LAST_VARIANT.create();
        }
        feedback(ctx, "Deleted variant #" + n + " (" + PickerState.variantCount() + " left).");
        return 1;
    }

    private static int variantNew(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = PickerState.newVariant();
        feedback(ctx, "Added variant #" + n + " (" + PickerState.variantCount() + " total). Now editing it.");
        return 1;
    }

    private static int variantSelect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.selectVariant(n)) {
            throw BAD_VARIANT.create(n);
        }
        feedback(ctx, "Editing variant #" + n + " (" + PickerState.currentEyeCount() + " eyes).");
        return 1;
    }

    private static int variantWeight(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float w = FloatArgumentType.getFloat(ctx, "w");
        PickerState.setVariantWeight(w);
        feedback(ctx, "Variant #" + (PickerState.variantIndex + 1) + " weight = " + w + ".");
        return 1;
    }

    /** Add a child literal {@code name} to {@code root}, letting {@code config} build out its subtree. */
    private static void verb(LiteralArgumentBuilder<CommandSourceStack> root, String name,
                             Consumer<LiteralArgumentBuilder<CommandSourceStack>> config) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name);
        config.accept(node);
        root.then(node);
    }
}
