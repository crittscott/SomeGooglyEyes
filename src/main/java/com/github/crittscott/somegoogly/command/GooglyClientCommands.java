package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import com.github.crittscott.somegoogly.picker.PickerExporter;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.picker.PickerState.ListedEye;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * <p>Each verb has a short and a full literal sharing one builder (e.g. {@code mv}/{@code move}). The
 * picker keyboard and this CLI call the same {@link PickerState} methods, so they stay in lock-step.
 */
public class GooglyClientCommands {

    private static final SimpleCommandExceptionType SINGLEPLAYER_ONLY =
            new SimpleCommandExceptionType(Component.literal("Spawn-all only works in single-player."));
    private static final SimpleCommandExceptionType NOT_CREATIVE =
            new SimpleCommandExceptionType(Component.literal("The picker requires creative mode."));
    private static final SimpleCommandExceptionType NOT_CHOSEN =
            new SimpleCommandExceptionType(Component.literal("Choose a mob first (/sg choose)."));
    private static final SimpleCommandExceptionType NO_PART =
            new SimpleCommandExceptionType(Component.literal("Pick a part first (/sg part <name>)."));
    private static final DynamicCommandExceptionType BAD_PART =
            new DynamicCommandExceptionType(t -> Component.literal("No such part: " + t));
    private static final DynamicCommandExceptionType BAD_INDEX =
            new DynamicCommandExceptionType(n -> Component.literal("No eye #" + n + "."));
    private static final SimpleCommandExceptionType NO_TARGET =
            new SimpleCommandExceptionType(Component.literal("Look at a mob first."));
    private static final SimpleCommandExceptionType NO_EYES =
            new SimpleCommandExceptionType(Component.literal("That mob has no eye config."));

    /** Captured at registration so chained fragments can be re-dispatched. */
    private static CommandDispatcher<CommandSourceStack> commandDispatcher;

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        commandDispatcher = event.getDispatcher();
        register(commandDispatcher);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> sg = Commands.literal("sg");

        alias(sg, "ch", "choose", b -> terminal(b, GooglyClientCommands::choose));
        alias(sg, "un", "unchoose", b -> terminal(b, GooglyClientCommands::unchoose));

        alias(sg, "pa", "part", b -> {
            RequiredArgumentBuilder<CommandSourceStack, String> target =
                    Commands.argument("target", StringArgumentType.word());
            terminal(target, GooglyClientCommands::part);
            b.then(target);
        });

        alias(sg, "cr", "create", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Float> z =
                    Commands.argument("z", FloatArgumentType.floatArg());
            terminal(z, GooglyClientCommands::create);
            b.then(Commands.argument("x", FloatArgumentType.floatArg())
                    .then(Commands.argument("y", FloatArgumentType.floatArg()).then(z)));
        });

        // move: mv / mo / move (all the same), with ~ supported per axis.
        Consumer<LiteralArgumentBuilder<CommandSourceStack>> moveCfg = b -> {
            RequiredArgumentBuilder<CommandSourceStack, Optional<Float>> z =
                    Commands.argument("z", MaybeFloatArgumentType.maybeFloat());
            terminal(z, GooglyClientCommands::move);
            b.then(Commands.argument("x", MaybeFloatArgumentType.maybeFloat())
                    .then(Commands.argument("y", MaybeFloatArgumentType.maybeFloat()).then(z)));
        };
        addAlias(sg, "mv", moveCfg);
        addAlias(sg, "mo", moveCfg);
        addAlias(sg, "move", moveCfg);

        alias(sg, "ro", "rot", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Optional<Float>> azimuth =
                    Commands.argument("azimuth", MaybeFloatArgumentType.maybeFloat());
            terminal(azimuth, GooglyClientCommands::rot);
            b.then(Commands.argument("inclination", MaybeFloatArgumentType.maybeFloat()).then(azimuth));
        });

        alias(sg, "sa", "save", b -> terminal(b, GooglyClientCommands::save));

        alias(sg, "se", "select", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
            terminal(n, GooglyClientCommands::select);
            b.then(n);
        });
        alias(sg, "de", "delete", b -> {
            RequiredArgumentBuilder<CommandSourceStack, Integer> n = Commands.argument("n", IntegerArgumentType.integer(1));
            terminal(n, GooglyClientCommands::delete);
            b.then(n);
        });

        alias(sg, "li", "list", b -> {
            addAlias(b, "pa", x -> terminal(x, GooglyClientCommands::listParts));
            addAlias(b, "parts", x -> terminal(x, GooglyClientCommands::listParts));
            addAlias(b, "ey", x -> terminal(x, GooglyClientCommands::listEyes));
            addAlias(b, "eyes", x -> terminal(x, GooglyClientCommands::listEyes));
        });

        alias(sg, "pr", "properties", b -> {
            b.then(prop("eyescale", FloatArgumentType.floatArg(0), GooglyClientCommands::propEyeScale));
            b.then(prop("irisscale", FloatArgumentType.floatArg(0), GooglyClientCommands::propIrisScale));
            b.then(Commands.literal("corneacolor").then(rgb(GooglyClientCommands::propCorneaColor)));
            b.then(Commands.literal("iriscolor").then(rgb(GooglyClientCommands::propIrisColor)));
            b.then(prop("glow", BoolArgumentType.bool(), GooglyClientCommands::propGlow));
            b.then(prop("invis", BoolArgumentType.bool(), GooglyClientCommands::propInvis));
        });

        alias(sg, "ex", "export", b -> terminal(b, GooglyClientCommands::export));

        // debug: force the client-side Keystone B expressions on the looked-at mob (testing only).
        alias(sg, "db", "debug", b -> {
            LiteralArgumentBuilder<CommandSourceStack> blink = Commands.literal("blink");
            terminal(blink, ctx -> debugBlink(ctx, false));
            addAlias(blink, "wink", x -> terminal(x, ctx -> debugBlink(ctx, true)));
            b.then(blink);

            LiteralArgumentBuilder<CommandSourceStack> stare = Commands.literal("stare");
            terminal(stare, ctx -> debugStare(ctx, 60));
            RequiredArgumentBuilder<CommandSourceStack, Integer> ticks = Commands.argument("ticks", IntegerArgumentType.integer(1));
            terminal(ticks, ctx -> debugStare(ctx, IntegerArgumentType.getInteger(ctx, "ticks")));
            stare.then(ticks);
            b.then(stare);

            addAlias(b, "swirl", x -> terminal(x, GooglyClientCommands::debugSwirl));

            LiteralArgumentBuilder<CommandSourceStack> anger = Commands.literal("anger");
            RequiredArgumentBuilder<CommandSourceStack, Boolean> on = Commands.argument("on", BoolArgumentType.bool());
            terminal(on, GooglyClientCommands::debugAnger);
            anger.then(on);
            b.then(anger);
        });

        // spawnall has no short form (and no chain tail) — the full word is required, one-off only.
        sg.then(Commands.literal("spawnall").executes(GooglyClientCommands::spawnAll));

        dispatcher.register(sg);
    }

    // ---- builder helpers -------------------------------------------------------------------

    private static void alias(LiteralArgumentBuilder<CommandSourceStack> root, String shortName, String fullName,
                              Consumer<LiteralArgumentBuilder<CommandSourceStack>> config) {
        addAlias(root, shortName, config);
        addAlias(root, fullName, config);
    }

    private static void addAlias(LiteralArgumentBuilder<CommandSourceStack> root, String name,
                                 Consumer<LiteralArgumentBuilder<CommandSourceStack>> config) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name);
        config.accept(node);
        root.then(node);
    }

    private static <T> LiteralArgumentBuilder<CommandSourceStack> prop(String name, ArgumentType<T> type,
                                                                       Command<CommandSourceStack> exec) {
        RequiredArgumentBuilder<CommandSourceStack, T> v = Commands.argument("v", type);
        terminal(v, exec);
        return Commands.literal(name).then(v);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Float> rgb(Command<CommandSourceStack> exec) {
        RequiredArgumentBuilder<CommandSourceStack, Float> bArg = Commands.argument("b", FloatArgumentType.floatArg(0, 1));
        terminal(bArg, exec);
        return Commands.argument("r", FloatArgumentType.floatArg(0, 1))
                .then(Commands.argument("g", FloatArgumentType.floatArg(0, 1)).then(bArg));
    }

    /**
     * Make a command's terminal node both executable on its own and the head of an {@code &}-separated
     * chain: {@code <args>} runs {@code leaf}; {@code <args> & <more>} runs {@code leaf} then dispatches
     * {@code /sg <more>}, which re-enters the tree and peels off the next fragment. A space must precede
     * the {@code &} (so Brigadier ends the preceding argument); a space must follow it (the {@code &}
     * literal needs a separator).
     */
    private static void terminal(ArgumentBuilder<CommandSourceStack, ?> node, Command<CommandSourceStack> leaf) {
        node.executes(leaf);
        node.then(Commands.literal("&").then(
                Commands.argument("rest", StringArgumentType.greedyString()).executes(chain(leaf))));
    }

    private static Command<CommandSourceStack> chain(Command<CommandSourceStack> leaf) {
        return ctx -> {
            leaf.run(ctx); // this fragment's own work first
            String rest = StringArgumentType.getString(ctx, "rest").trim();
            if (rest.isEmpty()) {
                return 1;
            }
            return commandDispatcher.execute("sg " + rest, ctx.getSource());
        };
    }

    // ---- executors -------------------------------------------------------------------------

    private static int choose(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        PickerState.active = true; // turn the picker on so the preview/gizmo render
        feedback(ctx, PickerState.lockOn());
        return 1;
    }

    private static int unchoose(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        PickerState.unlock();
        feedback(ctx, "Selection cleared.");
        return 1;
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

    private static int move(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setPosition(opt(MaybeFloatArgumentType.get(ctx, "x")),
                opt(MaybeFloatArgumentType.get(ctx, "y")),
                opt(MaybeFloatArgumentType.get(ctx, "z")));
        EyeConfig e = PickerState.currentEye;
        feedback(ctx, String.format("Position [%.3f, %.3f, %.3f].", e.position[0], e.position[1], e.position[2]));
        return 1;
    }

    private static int rot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setRotation(opt(MaybeFloatArgumentType.get(ctx, "inclination")),
                opt(MaybeFloatArgumentType.get(ctx, "azimuth")));
        EyeConfig e = PickerState.currentEye;
        feedback(ctx, String.format("Rotation incl %.1f° azi %.1f°.", incl(e), azi(e)));
        return 1;
    }

    private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        if (!PickerState.save()) {
            throw NO_PART.create();
        }
        feedback(ctx, "Saved eye #" + (PickerState.selectedIndex + 1) + " (" + PickerState.committedCount() + " total).");
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

    private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!PickerState.delete(n)) {
            throw BAD_INDEX.create(n);
        }
        feedback(ctx, "Deleted eye #" + n + " (" + PickerState.committedCount() + " left).");
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

    private static int listEyes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        List<ListedEye> list = PickerState.eyes;
        feedback(ctx, "Eyes (" + list.size() + "):");
        for (int i = 0; i < list.size(); i++) {
            ListedEye le = list.get(i);
            EyeConfig e = le.eye;
            String mark = i == PickerState.selectedIndex ? " *" : "";
            feedback(ctx, String.format("  %d. part=%s pos[%.3f, %.3f, %.3f] incl %.1f azi %.1f%s",
                    i + 1, le.part, e.position[0], e.position[1], e.position[2], incl(e), azi(e), mark));
        }
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

    private static int propIrisScale(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        float v = FloatArgumentType.getFloat(ctx, "v");
        PickerState.setIrisScale(v);
        feedback(ctx, "irisScale = " + v);
        return 1;
    }

    private static int propCorneaColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        requireChosen();
        PickerState.setCorneaColor(FloatArgumentType.getFloat(ctx, "r"),
                FloatArgumentType.getFloat(ctx, "g"), FloatArgumentType.getFloat(ctx, "b"));
        feedback(ctx, "corneaColors set.");
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

    private static int export(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        feedback(ctx, PickerExporter.export());
        return 1;
    }

    private static int spawnAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        requireCreative();
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            throw SINGLEPLAYER_ONLY.create();
        }
        // Spawning is server-side work; resolve the server-side player on the server thread and run there.
        UUID uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                SpawnAllCommand.spawn(player);
            }
        });
        feedback(ctx, "Spawning mobs…");
        return 1;
    }

    // ---- debug (Keystone B expression triggers) --------------------------------------------

    private static int debugBlink(CommandContext<CommandSourceStack> ctx, boolean wink) throws CommandSyntaxException {
        lookedAtTracker().forceBlink(wink);
        feedback(ctx, wink ? "Wink." : "Blink.");
        return 1;
    }

    private static int debugStare(CommandContext<CommandSourceStack> ctx, int ticks) throws CommandSyntaxException {
        lookedAtTracker().forceStare(ticks);
        feedback(ctx, "Staring for " + ticks + " ticks.");
        return 1;
    }

    private static int debugSwirl(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        lookedAtTracker().forceSwirl();
        feedback(ctx, "Swirl.");
        return 1;
    }

    private static int debugAnger(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        boolean on = BoolArgumentType.getBool(ctx, "on");
        lookedAtTracker().forceAnger(on);
        feedback(ctx, "Anger " + (on ? "on" : "off") + ".");
        return 1;
    }

    /** Resolve the tracker for the mob under the crosshair (creative-gated, like the picker). */
    private static GooglyTracker lookedAtTracker() throws CommandSyntaxException {
        requireCreative();
        LivingEntity living = lookedAtLiving();
        if (living == null) {
            throw NO_TARGET.create();
        }
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        HeadInfo helper = HeadInfo.getHelper(type, living);
        if (helper == null || !helper.hasConfig()) {
            throw NO_EYES.create();
        }
        return SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
    }

    /** The mob under the crosshair, using the game's own entity pick (matches what you see). */
    private static LivingEntity lookedAtLiving() {
        Entity target = Minecraft.getInstance().crosshairPickEntity;
        return target instanceof LivingEntity living ? living : null;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static Double opt(Optional<Float> value) {
        return value.map(Float::doubleValue).orElse(null);
    }

    private static double incl(EyeConfig e) {
        return e.inclination != null ? e.inclination : HeadInfo.DEFAULT_INCLINATION;
    }

    private static double azi(EyeConfig e) {
        return e.azimuth != null ? e.azimuth : HeadInfo.DEFAULT_AZIMUTH;
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal("[Googly] " + text), false);
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
}
