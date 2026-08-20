package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.client.picker.PickerExporter;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.picker.PickerState.ListedEye;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerMobPosePacket;
import com.github.crittscott.somegoogly.network.PickerSpawnAllPacket;
import com.github.crittscott.somegoogly.network.PickerSpawnPacket;
import com.github.crittscott.somegoogly.picker.PickerSpawnService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The {@code /sg} command tree — the in-game eye-placement CLI. Registered as <b>client</b> commands
 * (via Architectury's client command event) because the editing they drive lives entirely in the
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
            new DynamicCommandExceptionType(n -> Component.translatable("somegoogly.command.picker.bad_cross_target", n));
    private static final DynamicCommandExceptionType BAD_INDEX =
            new DynamicCommandExceptionType(n -> Component.translatable("somegoogly.command.picker.bad_index", n));
    private static final DynamicCommandExceptionType BAD_PART =
            new DynamicCommandExceptionType(t -> Component.translatable("somegoogly.command.picker.bad_part", t));
    private static final DynamicCommandExceptionType BAD_VARIANT =
            new DynamicCommandExceptionType(n -> Component.translatable("somegoogly.command.picker.bad_variant", n));
    private static final SimpleCommandExceptionType CANT_DELETE_LAST_VARIANT =
            new SimpleCommandExceptionType(Component.translatable("somegoogly.command.picker.cant_delete_last_variant"));
    private static final SimpleCommandExceptionType NO_DRAFT =
            new SimpleCommandExceptionType(Component.translatable("somegoogly.command.picker.no_draft"));
    private static final SimpleCommandExceptionType NO_PART =
            new SimpleCommandExceptionType(Component.translatable("somegoogly.command.picker.no_part"));
    private static final SimpleCommandExceptionType NOT_CHOSEN =
            new SimpleCommandExceptionType(Component.translatable("somegoogly.command.picker.not_chosen"));
    private static final SimpleCommandExceptionType NOT_CREATIVE =
            new SimpleCommandExceptionType(Component.translatable("somegoogly.command.picker.not_creative"));

    private static int choose(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        PickerState.activate(); // turn the picker on so the preview/gizmo render
        feedback(ctx, "somegoogly.command.picker.feedback", PickerState.lockOn());
        return 1;
    }

    private static int create(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float x = FloatArgumentType.getFloat(ctx, "x");
        float y = FloatArgumentType.getFloat(ctx, "y");
        float z = FloatArgumentType.getFloat(ctx, "z");
        PickerState.createEye(x, y, z);
        feedback(ctx, "somegoogly.command.picker.created_eye",
                String.format("%.3f", x), String.format("%.3f", y), String.format("%.3f", z));
        return 1;
    }

    private static int delete(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.delete(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "somegoogly.command.picker.deleted_eye", n, PickerState.currentEyeCount());
        return 1;
    }

    private static int dupe(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.dupe(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "somegoogly.command.picker.duped_eye", n);
        return 1;
    }

    /** Exports the chosen mob's draft; {@code requireChosen} keeps it from firing at a stale target. */
    private static int export(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        feedback(ctx, "somegoogly.command.picker.feedback", PickerExporter.export());
        return 1;
    }

    private static int exportAll(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        feedback(ctx, "somegoogly.command.picker.feedback", PickerExporter.exportAll());
        return 1;
    }

    private static void feedback(CommandContext<ClientCommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().arch$sendSuccess(() -> Component.translatable(key, args), false);
    }

    private static int listEyes(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        List<ListedEye> list = PickerState.currentEyes();
        feedback(ctx, "somegoogly.command.picker.eyes_header", PickerState.variantIndex() + 1, list.size());
        for (int i = 0; i < list.size(); i++) {
            ListedEye le = list.get(i);
            EyeDraft e = le.eye;
            String mark = i == PickerState.selectedIndex() ? " *" : "";
            String cross = e.crossTarget >= 0 ? " X→" + (e.crossTarget + 1) : "";
            feedback(ctx, "somegoogly.command.picker.eye_entry", i + 1, le.part,
                    String.format("%.3f", e.position[0]), String.format("%.3f", e.position[1]), String.format("%.3f", e.position[2]),
                    String.format("%.0f", e.inclination), String.format("%.0f", e.azimuth), cross, mark);
        }
        return 1;
    }

    private static int listParts(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        feedback(ctx, "somegoogly.command.picker.parts_header", PickerState.parts().size());
        for (int i = 0; i < PickerState.parts().size(); i++) {
            feedback(ctx, "somegoogly.command.picker.part_entry", i + 1, PickerState.parts().get(i));
        }
        return 1;
    }

    private static int listVariants(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        feedback(ctx, "somegoogly.command.picker.variants_header", PickerState.variantCount());
        for (int i = 0; i < PickerState.variantCount(); i++) {
            PickerState.DraftVariant v = PickerState.variants().get(i);
            String mark = i == PickerState.variantIndex() ? " *" : "";
            feedback(ctx, "somegoogly.command.picker.variant_entry", i + 1, String.format("%.2f", v.weight), v.eyes.size(), mark);
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
    private static int mobMove(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        LivingEntity target = PickerState.target();

        double dx = FloatArgumentType.getFloat(ctx, "dx");
        double dy = FloatArgumentType.getFloat(ctx, "dy");
        double dz = FloatArgumentType.getFloat(ctx, "dz");

        NetworkHandler.sendToServer(PickerMobPosePacket.move(target.getUUID(), dx, dy, dz));
        return 1;
    }

    /**
     * The CLI {@code mob rot <azimuth>} op: turn the chosen mob in the XZ plane, via
     * {@link PickerMobPosePacket}. {@code azimuth} uses the <b>eye</b> convention (degrees from +X;
     * 270 = facing -Z) so its numbers mean the same direction as {@code /sg rot}; the server handler
     * converts to Minecraft yaw and reports back.
     */
    private static int mobRot(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float azimuth = FloatArgumentType.getFloat(ctx, "azimuth");
        NetworkHandler.sendToServer(PickerMobPosePacket.rot(PickerState.target().getUUID(), azimuth));
        return 1;
    }

    private static int move(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setPosition(MaybeFloatArgumentType.get(ctx, "x"),
                MaybeFloatArgumentType.get(ctx, "y"),
                MaybeFloatArgumentType.get(ctx, "z"));
        EyeDraft e = PickerState.currentEye();
        feedback(ctx, "somegoogly.command.picker.position",
                String.format("%.3f", e.position[0]), String.format("%.3f", e.position[1]), String.format("%.3f", e.position[2]));
        return 1;
    }

    private static int part(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        String target = StringArgumentType.getString(ctx, "target");
        if (target.equalsIgnoreCase("none")) {
            PickerState.clearPart();
            feedback(ctx, "somegoogly.command.picker.part_cleared");
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
        feedback(ctx, "somegoogly.command.picker.part_set", PickerState.currentPart());
        return 1;
    }

    private static int posrot(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setPosition(MaybeFloatArgumentType.get(ctx, "x"),
                MaybeFloatArgumentType.get(ctx, "y"),
                MaybeFloatArgumentType.get(ctx, "z"));
        PickerState.setRotation(MaybeFloatArgumentType.get(ctx, "inclination"),
                MaybeFloatArgumentType.get(ctx, "azimuth"));
        EyeDraft e = PickerState.currentEye();
        feedback(ctx, "somegoogly.command.picker.posrot",
                String.format("%.3f", e.position[0]), String.format("%.3f", e.position[1]), String.format("%.3f", e.position[2]),
                String.format("%.0f", e.inclination), String.format("%.0f", e.azimuth));
        return 1;
    }

    private static <T> LiteralArgumentBuilder<ClientCommandSourceStack> prop(String name, ArgumentType<T> type,
                                                                       Command<ClientCommandSourceStack> exec) {
        RequiredArgumentBuilder<ClientCommandSourceStack, T> v = ClientCommandRegistrationEvent.argument("v", type);
        v.executes(exec);
        return ClientCommandRegistrationEvent.literal(name).then(v);
    }

    private static int propCrossTarget(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        int n = IntegerArgumentType.getInteger(ctx, "v");
        if (!PickerState.setCrossTarget(n)) {
            throw BAD_CROSS_TARGET.create(n);
        }
        int target = PickerState.currentEye().crossTarget;
        if (target < 0) {
            feedback(ctx, "somegoogly.command.picker.cross_target_cleared");
        } else {
            feedback(ctx, "somegoogly.command.picker.cross_target_set", target + 1);
        }
        return 1;
    }

    private static int propCorneaColor(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setCorneaColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "somegoogly.command.picker.cornea_set");
        return 1;
    }

    private static int propDepth(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setDepth(v);
        feedback(ctx, "somegoogly.command.picker.depth_set", v);
        return 1;
    }

    private static int propEyeScale(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setEyeScale(v);
        feedback(ctx, "somegoogly.command.picker.eyescale_set", v);
        return 1;
    }

    private static int propGlow(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        boolean v = BoolArgumentType.getBool(ctx, "v");
        PickerState.setGlow(v);
        feedback(ctx, "somegoogly.command.picker.glow_set", v);
        return 1;
    }

    private static int propIrisColor(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setIrisColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "somegoogly.command.picker.iris_set");
        return 1;
    }

    private static int propIrisScale(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setIrisScale(v);
        feedback(ctx, "somegoogly.command.picker.irisscale_set", v);
        return 1;
    }

    public static void register(CommandDispatcher<ClientCommandSourceStack> dispatcher, CommandBuildContext context) {
        LiteralArgumentBuilder<ClientCommandSourceStack> sg = ClientCommandRegistrationEvent.literal("sg");

        verb(sg, "choose", b -> b.executes(GooglyClientCommands::choose));
        verb(sg, "unchoose", b -> b.executes(GooglyClientCommands::unchoose));

        verb(sg, "part", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, String> target =
                    ClientCommandRegistrationEvent.argument("target", StringArgumentType.word());
            target.executes(GooglyClientCommands::part);
            b.then(target);
        });

        verb(sg, "create", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Float> z =
                    ClientCommandRegistrationEvent.argument("z", FloatArgumentType.floatArg());
            z.executes(GooglyClientCommands::create);
            b.then(ClientCommandRegistrationEvent.argument("x", FloatArgumentType.floatArg())
                    .then(ClientCommandRegistrationEvent.argument("y", FloatArgumentType.floatArg()).then(z)));
        });

        // move: ~ supported per axis to leave an axis unchanged.
        verb(sg, "move", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Optional<Float>> z =
                    ClientCommandRegistrationEvent.argument("z", MaybeFloatArgumentType.maybeFloat());
            z.executes(GooglyClientCommands::move);
            b.then(ClientCommandRegistrationEvent.argument("x", MaybeFloatArgumentType.maybeFloat())
                    .then(ClientCommandRegistrationEvent.argument("y", MaybeFloatArgumentType.maybeFloat()).then(z)));
        });

        verb(sg, "rot", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Optional<Float>> azimuth =
                    ClientCommandRegistrationEvent.argument("azimuth", MaybeFloatArgumentType.maybeFloat());
            azimuth.executes(GooglyClientCommands::rot);
            b.then(ClientCommandRegistrationEvent.argument("inclination", MaybeFloatArgumentType.maybeFloat()).then(azimuth));
        });

        // posrot: move + rot in one go (the common case). ~ supported per component to leave it unchanged.
        verb(sg, "posrot", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Optional<Float>> azimuth =
                    ClientCommandRegistrationEvent.argument("azimuth", MaybeFloatArgumentType.maybeFloat());
            azimuth.executes(GooglyClientCommands::posrot);
            b.then(ClientCommandRegistrationEvent.argument("x", MaybeFloatArgumentType.maybeFloat())
                    .then(ClientCommandRegistrationEvent.argument("y", MaybeFloatArgumentType.maybeFloat())
                            .then(ClientCommandRegistrationEvent.argument("z", MaybeFloatArgumentType.maybeFloat())
                                    .then(ClientCommandRegistrationEvent.argument("inclination", MaybeFloatArgumentType.maybeFloat())
                                            .then(azimuth)))));
        });

        // mob move/rot: reposition or turn the chosen mob itself — distinct from the eye move/rot
        // verbs above (mob move takes offsets, not absolutes). Lives under its own 'mob' literal
        // (not the server-side 'admin' subtree) so the client and server /sg trees keep disjoint
        // paths and command fall-through keeps working.
        verb(sg, "mob", b -> {
            verb(b, "move", x -> {
                RequiredArgumentBuilder<ClientCommandSourceStack, Float> dz =
                        ClientCommandRegistrationEvent.argument("dz", FloatArgumentType.floatArg());
                dz.executes(GooglyClientCommands::mobMove);
                x.then(ClientCommandRegistrationEvent.argument("dx", FloatArgumentType.floatArg())
                        .then(ClientCommandRegistrationEvent.argument("dy", FloatArgumentType.floatArg()).then(dz)));
            });
            verb(b, "rot", x -> {
                RequiredArgumentBuilder<ClientCommandSourceStack, Float> azimuth =
                        ClientCommandRegistrationEvent.argument("azimuth", FloatArgumentType.floatArg());
                azimuth.executes(GooglyClientCommands::mobRot);
                x.then(azimuth);
            });
        });

        verb(sg, "save", b -> b.executes(GooglyClientCommands::save));

        verb(sg, "select", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Integer> n = ClientCommandRegistrationEvent.argument("n", IntegerArgumentType.integer(1));
            n.executes(GooglyClientCommands::select);
            b.then(n);
        });
        verb(sg, "delete", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Integer> n = ClientCommandRegistrationEvent.argument("n", IntegerArgumentType.integer(1));
            n.executes(GooglyClientCommands::delete);
            b.then(n);
        });
        // dupe <n>: copy a saved eye into a new unsaved draft (select without the selection).
        verb(sg, "dupe", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, Integer> n = ClientCommandRegistrationEvent.argument("n", IntegerArgumentType.integer(1));
            n.executes(GooglyClientCommands::dupe);
            b.then(n);
        });

        // variant: new / <n> (switch) / del <n> / weight <w>. Literals resolve before the bare integer arg.
        verb(sg, "variant", b -> {
            verb(b, "new", x -> x.executes(GooglyClientCommands::variantNew));
            verb(b, "weight", x -> {
                RequiredArgumentBuilder<ClientCommandSourceStack, Float> w = ClientCommandRegistrationEvent.argument("w", FloatArgumentType.floatArg(0));
                w.executes(GooglyClientCommands::variantWeight);
                x.then(w);
            });
            verb(b, "del", x -> {
                RequiredArgumentBuilder<ClientCommandSourceStack, Integer> n = ClientCommandRegistrationEvent.argument("n", IntegerArgumentType.integer(1));
                n.executes(GooglyClientCommands::variantDelete);
                x.then(n);
            });
            RequiredArgumentBuilder<ClientCommandSourceStack, Integer> n = ClientCommandRegistrationEvent.argument("n", IntegerArgumentType.integer(1));
            n.executes(GooglyClientCommands::variantSelect);
            b.then(n);
        });

        verb(sg, "list", b -> {
            verb(b, "parts", x -> x.executes(GooglyClientCommands::listParts));
            verb(b, "eyes", x -> x.executes(GooglyClientCommands::listEyes));
            verb(b, "variants", x -> x.executes(GooglyClientCommands::listVariants));
        });

        verb(sg, "properties", b -> {
            b.then(prop("eyescale", FloatArgumentType.floatArg(0), GooglyClientCommands::propEyeScale));
            b.then(prop("irisscale", FloatArgumentType.floatArg(0), GooglyClientCommands::propIrisScale));
            // depth <v>: thickness multiplier along the look axis (1 = standard proportions).
            b.then(prop("depth", FloatArgumentType.floatArg(0), GooglyClientCommands::propDepth));
            b.then(ClientCommandRegistrationEvent.literal("corneacolor").then(rgb(GooglyClientCommands::propCorneaColor)));
            b.then(ClientCommandRegistrationEvent.literal("iriscolor").then(rgb(GooglyClientCommands::propIrisColor)));
            b.then(prop("glow", BoolArgumentType.bool(), GooglyClientCommands::propGlow));
            // crosstarget <n>: cross-eye partner as a 1-based eye list index; 0 clears it.
            b.then(prop("crosstarget", IntegerArgumentType.integer(0), GooglyClientCommands::propCrossTarget));
        });

        verb(sg, "export", b -> b.executes(GooglyClientCommands::export));
        verb(sg, "exportall", b -> b.executes(GooglyClientCommands::exportAll));

        // Behavior testing lives in the server-side /sg admin command (the schedule is server-owned).

        // spawn <type> — one mob at the block the player is targeting; the single-mob sibling of
        // spawnall. The registry argument gives validation + tab completion of summonable types.
        verb(sg, "spawn", b -> {
            RequiredArgumentBuilder<ClientCommandSourceStack, ?> type =
                    ClientCommandRegistrationEvent.argument("type", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                                            .filter(id -> BuiltInRegistries.ENTITY_TYPE.get(id).canSummon()),
                                    builder));
            type.executes(GooglyClientCommands::spawn);
            b.then(type);
        });

        // spawnall [mod] — bare spawns every mod; an optional namespace narrows it (a debugging aid).
        verb(sg, "spawnall", b -> {
            b.executes(GooglyClientCommands::spawnAll);
            RequiredArgumentBuilder<ClientCommandSourceStack, String> mod =
                    ClientCommandRegistrationEvent.argument("mod", StringArgumentType.word());
            mod.executes(GooglyClientCommands::spawnAll);
            b.then(mod);
        });

        dispatcher.register(sg);
    }

    private static void requireChosen() throws CommandSyntaxException {
        if (!PickerState.isActive() || PickerState.target() == null) {
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

    private static RequiredArgumentBuilder<ClientCommandSourceStack, Float> rgb(Command<ClientCommandSourceStack> exec) {
        RequiredArgumentBuilder<ClientCommandSourceStack, Float> bArg = ClientCommandRegistrationEvent.argument("b", FloatArgumentType.floatArg(0, 1));
        bArg.executes(exec);
        return ClientCommandRegistrationEvent.argument("r", FloatArgumentType.floatArg(0, 1))
                .then(ClientCommandRegistrationEvent.argument("g", FloatArgumentType.floatArg(0, 1)).then(bArg));
    }

    private static int rot(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        PickerState.setRotation(MaybeFloatArgumentType.get(ctx, "inclination"),
                MaybeFloatArgumentType.get(ctx, "azimuth"));
        EyeDraft e = PickerState.currentEye();
        feedback(ctx, "somegoogly.command.picker.rotation",
                String.format("%.0f", e.inclination), String.format("%.0f", e.azimuth));
        return 1;
    }

    private static int save(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        requireDraft();
        if (!PickerState.save()) {
            throw NO_PART.create();
        }
        feedback(ctx, "somegoogly.command.picker.saved_eye",
                PickerState.selectedIndex() + 1, PickerState.variantIndex() + 1, PickerState.currentEyeCount());
        return 1;
    }

    private static int select(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.select(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "somegoogly.command.picker.editing_eye", n);
        return 1;
    }

    /**
     * The CLI {@code spawn <type>} op: spawn one mob at the block the player is targeting (NoAi +
     * persistent, like {@code spawnall}). Validation/suggestions are vanilla's summonable set;
     * placement, fit-checking, and feedback are the server-side half ({@link PickerSpawnService#spawnOne},
     * reached via {@code PickerSpawnPacket}).
     */
    private static int spawn(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        ResourceLocation type = ctx.getArgument("type", ResourceLocation.class);
        NetworkHandler.sendToServer(new PickerSpawnPacket(type));
        return 1;
    }

    private static int spawnAll(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        // The mod-namespace filter is optional; absent when the bare `spawnall` node executes.
        String mod;
        try {
            mod = StringArgumentType.getString(ctx, "mod");
        } catch (IllegalArgumentException noArg) {
            mod = null;
        }
        // Spawning is server-side work ({@link PickerSpawnService#spawn}, reached via PickerSpawnAllPacket).
        NetworkHandler.sendToServer(new PickerSpawnAllPacket(mod));
        if (mod == null) {
            feedback(ctx, "somegoogly.command.picker.spawning_all");
        } else {
            feedback(ctx, "somegoogly.command.picker.spawning_mod", mod);
        }
        return 1;
    }

    private static int unchoose(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        PickerState.unlock();
        feedback(ctx, "somegoogly.command.picker.selection_cleared");
        return 1;
    }

    private static int variantDelete(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (n > PickerState.variantCount()) {
            throw BAD_VARIANT.create(n);
        }
        if (!PickerState.deleteVariant(n)) {
            throw CANT_DELETE_LAST_VARIANT.create();
        }
        feedback(ctx, "somegoogly.command.picker.deleted_variant", n, PickerState.variantCount());
        return 1;
    }

    private static int variantNew(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = PickerState.newVariant();
        feedback(ctx, "somegoogly.command.picker.added_variant", n, PickerState.variantCount());
        return 1;
    }

    private static int variantSelect(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.selectVariant(n)) {
            throw BAD_VARIANT.create(n);
        }
        feedback(ctx, "somegoogly.command.picker.editing_variant", n, PickerState.currentEyeCount());
        return 1;
    }

    private static int variantWeight(CommandContext<ClientCommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float w = FloatArgumentType.getFloat(ctx, "w");
        PickerState.setVariantWeight(w);
        feedback(ctx, "somegoogly.command.picker.variant_weight", PickerState.variantIndex() + 1, w);
        return 1;
    }

    /** Add a child literal {@code name} to {@code root}, letting {@code config} build out its subtree. */
    private static void verb(LiteralArgumentBuilder<ClientCommandSourceStack> root, String name,
                             Consumer<LiteralArgumentBuilder<ClientCommandSourceStack>> config) {
        LiteralArgumentBuilder<ClientCommandSourceStack> node = ClientCommandRegistrationEvent.literal(name);
        config.accept(node);
        root.then(node);
    }
}
