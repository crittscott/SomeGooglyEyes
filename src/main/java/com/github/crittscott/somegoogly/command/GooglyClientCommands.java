package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.client.picker.PickerExporter;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.picker.PickerState.ListedEye;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerMobPosePacket;
import com.github.crittscott.somegoogly.network.PickerSpawnAllPacket;
import com.github.crittscott.somegoogly.network.PickerSpawnPacket;
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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The {@code /sg} command tree — the in-game eye-placement CLI. Registered as <b>client</b> commands
 * (via {@link RegisterClientCommandsEvent}) because the editing they drive lives entirely in the
 * client-side {@link PickerState}. Like the keyboard picker, they require <b>creative mode</b>.
 *
 * <p>Each verb is a single full literal; there are no short aliases. The picker keyboard and this CLI
 * call the same {@link PickerState} methods, so they stay in lock-step.
 *
 * <p>The verbs that touch server state ({@code spawn}, {@code spawnall}, {@code mob move}/{@code rot},
 * and {@code export}) send C2S picker packets; the server-side handlers re-authorize (creative mode)
 * and do the work on the server thread, so these verbs work from a remote client as well as in
 * single-player. Their result feedback arrives as server chat messages. The {@code mob} verbs are
 * deliberately <b>not</b> under {@code admin}: that literal belongs to the server-side tree
 * ({@link GooglyAdminCommand}), and reusing it client-side would break the disjoint-path contract
 * that lets command fall-through route each side's input correctly.
 */
public class GooglyClientCommands {

    private static final DynamicCommandExceptionType BAD_CROSS_TARGET =
            new DynamicCommandExceptionType(n -> Component.literal(
                    "Can't cross toward eye #" + n + " (must be a different eye on the same part)."));
    private static final DynamicCommandExceptionType BAD_INDEX =
            new DynamicCommandExceptionType(n -> Component.literal("No eye #" + n + "."));
    private static final DynamicCommandExceptionType BAD_PART =
            new DynamicCommandExceptionType(t -> Component.literal("No such part: " + t));
    private static final DynamicCommandExceptionType BAD_VARIANT =
            new DynamicCommandExceptionType(n -> Component.literal("No variant #" + n + "."));
    private static final SimpleCommandExceptionType CANT_DELETE_LAST_VARIANT =
            new SimpleCommandExceptionType(Component.literal("Can't delete the last variant — there's always at least one."));
    private static final SimpleCommandExceptionType NO_DRAFT =
            new SimpleCommandExceptionType(Component.literal(
                    "No eye being edited. Start one with /sg create <x> <y> <z> (or /sg select <n>)."));
    private static final SimpleCommandExceptionType NO_PART =
            new SimpleCommandExceptionType(Component.literal("Pick a part first (/sg part <name>)."));
    private static final SimpleCommandExceptionType NOT_CHOSEN =
            new SimpleCommandExceptionType(Component.literal("Choose a mob first (/sg choose)."));
    private static final SimpleCommandExceptionType NOT_CREATIVE =
            new SimpleCommandExceptionType(Component.literal("The picker requires creative mode."));

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

    private static int dupe(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.dupe(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "Duplicated eye #" + n + " as a new unsaved eye — move it, then /sg save.");
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
            String cross = e.crossTarget >= 0 ? " X→" + (e.crossTarget + 1) : "";
            feedback(ctx, String.format("  %d. part=%s pos[%.3f, %.3f, %.3f] incl %.0f azi %.0f%s%s",
                    i + 1, le.part, e.position[0], e.position[1], e.position[2], incl(e), azi(e), cross, mark));
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

    /**
     * The CLI {@code mob move <dx> <dy> <dz>} op: nudge the chosen mob by world-axis offsets
     * (0 leaves that axis unchanged) — unlike the eye {@code move} verb, which sets absolutes.
     * Sent to the server as a {@link PickerMobPosePacket}, whose handler resolves the offsets
     * against the authoritative entity, applies the move, and reports the resulting position;
     * the change syncs to viewers through vanilla entity tracking.
     */
    private static int mobMove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        LivingEntity target = PickerState.target();

        double dx = FloatArgumentType.getFloat(ctx, "dx");
        double dy = FloatArgumentType.getFloat(ctx, "dy");
        double dz = FloatArgumentType.getFloat(ctx, "dz");

        NetworkHandler.INSTANCE.sendToServer(PickerMobPosePacket.move(target.getUUID(), dx, dy, dz));
        return 1;
    }

    /**
     * The CLI {@code mob rot <azimuth>} op: turn the chosen mob in the XZ plane, via
     * {@link PickerMobPosePacket}. {@code azimuth} uses the <b>eye</b> convention (degrees from +X;
     * 270 = facing -Z) so its numbers mean the same direction as {@code /sg rot}; the server handler
     * converts to Minecraft yaw and reports back.
     */
    private static int mobRot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float azimuth = FloatArgumentType.getFloat(ctx, "azimuth");
        NetworkHandler.INSTANCE.sendToServer(PickerMobPosePacket.rot(PickerState.target().getUUID(), azimuth));
        return 1;
    }

    private static int move(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setPosition(opt(MaybeFloatArgumentType.get(ctx, "x")),
                opt(MaybeFloatArgumentType.get(ctx, "y")),
                opt(MaybeFloatArgumentType.get(ctx, "z")));
        EyeDraft e = PickerState.currentEye;
        feedback(ctx, String.format("Position [%.3f, %.3f, %.3f].", e.position[0], e.position[1], e.position[2]));
        return 1;
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
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
        requireDraft();
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

    private static int propCrossTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        int n = IntegerArgumentType.getInteger(ctx, "v");
        if (!PickerState.setCrossTarget(n)) {
            throw BAD_CROSS_TARGET.create(n);
        }
        int target = PickerState.currentEye.crossTarget;
        feedback(ctx, target < 0 ? "crossTarget cleared." : "crossTarget = eye #" + (target + 1) + ".");
        return 1;
    }

    private static int propCorneaColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setCorneaColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "corneaColors set.");
        return 1;
    }

    private static int propDepth(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setDepth(v);
        feedback(ctx, "depth = " + v);
        return 1;
    }

    private static int propEyeScale(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setEyeScale(v);
        feedback(ctx, "eyeScale = " + v);
        return 1;
    }

    private static int propGlow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        boolean v = BoolArgumentType.getBool(ctx, "v");
        PickerState.setGlow(v);
        feedback(ctx, "glow = " + v);
        return 1;
    }

    private static int propIrisColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setIrisColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "irisColors set.");
        return 1;
    }

    private static int propIrisScale(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setIrisScale(v);
        feedback(ctx, "irisScale = " + v);
        return 1;
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
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

        // mob move/rot: reposition or turn the chosen mob itself — distinct from the eye move/rot
        // verbs above (mob move takes offsets, not absolutes). Lives under its own 'mob' literal
        // (not the server-side 'admin' subtree) so the client and server /sg trees keep disjoint
        // paths and command fall-through keeps working.
        verb(sg, "mob", b -> {
            verb(b, "move", x -> {
                RequiredArgumentBuilder<CommandSourceStack, Float> dz =
                        Commands.argument("dz", FloatArgumentType.floatArg());
                terminal(dz, GooglyClientCommands::mobMove);
                x.then(Commands.argument("dx", FloatArgumentType.floatArg())
                        .then(Commands.argument("dy", FloatArgumentType.floatArg()).then(dz)));
            });
            verb(b, "rot", x -> {
                RequiredArgumentBuilder<CommandSourceStack, Float> azimuth =
                        Commands.argument("azimuth", FloatArgumentType.floatArg());
                terminal(azimuth, GooglyClientCommands::mobRot);
                x.then(azimuth);
            });
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
        // dupe <n>: copy a saved eye into a new unsaved draft (select without the selection).
        verb(sg, "dupe", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
            terminal(n, GooglyClientCommands::dupe);
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
            // depth <v>: thickness multiplier along the look axis (1 = standard proportions).
            b.then(prop("depth", FloatArgumentType.floatArg(0), GooglyClientCommands::propDepth));
            b.then(Commands.literal("corneacolor").then(rgb(GooglyClientCommands::propCorneaColor)));
            b.then(Commands.literal("iriscolor").then(rgb(GooglyClientCommands::propIrisColor)));
            b.then(prop("glow", BoolArgumentType.bool(), GooglyClientCommands::propGlow));
            // crosstarget <n>: cross-eye partner as a 1-based eye list index; 0 clears it.
            b.then(prop("crosstarget", IntegerArgumentType.integer(0), GooglyClientCommands::propCrossTarget));
        });

        verb(sg, "export", b -> terminal(b, GooglyClientCommands::export));
        verb(sg, "exportall", b -> terminal(b, GooglyClientCommands::exportAll));

        // Behavior testing lives in the server-side /sg admin command (the schedule is server-owned).

        // spawn <type> — one mob at the block the player is targeting; the single-mob sibling of
        // spawnall. The registry argument gives validation + tab completion of summonable types.
        verb(sg, "spawn", b -> {
            RequiredArgumentBuilder<CommandSourceStack, ?> type =
                    Commands.argument("type", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                            .suggests(SuggestionProviders.SUMMONABLE_ENTITIES);
            terminal(type, GooglyClientCommands::spawn);
            b.then(type);
        });

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

    /** Guard the shaping verbs (move/rot/properties/save): they need a live draft, never auto-vivify one. */
    private static void requireDraft() throws CommandSyntaxException {
        if (!PickerState.hasDraft()) {
            throw NO_DRAFT.create();
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
        requireDraft();
        PickerState.setRotation(opt(MaybeFloatArgumentType.get(ctx, "inclination")),
                opt(MaybeFloatArgumentType.get(ctx, "azimuth")));
        EyeDraft e = PickerState.currentEye;
        feedback(ctx, String.format("Rotation incl %.0f° azi %.0f°.", incl(e), azi(e)));
        return 1;
    }

    private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
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

    /**
     * The CLI {@code spawn <type>} op: spawn one mob at the block the player is targeting (NoAi +
     * persistent, like {@code spawnall}). Validation/suggestions are vanilla's summonable set;
     * placement, fit-checking, and feedback are the server-side half ({@link SpawnAllCommand#spawnOne},
     * reached via {@code PickerSpawnPacket}).
     */
    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        EntityType<?> type = ResourceArgument.getSummonableEntityType(ctx, "type").value();
        NetworkHandler.INSTANCE.sendToServer(new PickerSpawnPacket(BuiltInRegistries.ENTITY_TYPE.getKey(type)));
        return 1;
    }

    private static int spawnAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        // The mod-namespace filter is optional; absent when the bare `spawnall` node executes.
        String mod;
        try {
            mod = StringArgumentType.getString(ctx, "mod");
        } catch (IllegalArgumentException noArg) {
            mod = null;
        }
        // Spawning is server-side work ({@link SpawnAllCommand#spawn}, reached via PickerSpawnAllPacket).
        NetworkHandler.INSTANCE.sendToServer(new PickerSpawnAllPacket(mod));
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
