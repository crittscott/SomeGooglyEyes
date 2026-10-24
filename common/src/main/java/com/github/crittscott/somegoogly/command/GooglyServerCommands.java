package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import com.github.crittscott.somegoogly.picker.PickerSpawnService;
import com.github.crittscott.somegoogly.util.LookTarget;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Server-owned {@code /sg} commands. Both kinds require creative mode and permission level 2: the
 * picker branches ({@link PickerGate#mayUsePicker}) spawn, move, and rotate authoring mobs; the
 * {@code admin} subtree changes the looked-at entity's eye state or active cosmetic behavior.
 *
 * <p>The local editing branches use the client dispatcher. The two trees have disjoint child paths
 * under one {@code /sg} root; loader client adapters preserve routing to these server branches.
 *
 * <ul>
 *   <li>{@code /sg admin eyes <true|false>} — toggle the has-eyes flag</li>
 *   <li>{@code /sg admin tint iris <r> <g> <b>} / {@code tint cornea <r> <g> <b>} — set a color (0–1)</li>
 *   <li>{@code /sg admin tint clear} — drop both color overrides</li>
 *   <li>{@code /sg admin glow <on|off|config>} — force glow on/off, or revert to per-eye config</li>
 *   <li>{@code /sg admin behavior <id|random>} — trigger a cosmetic behavior now</li>
 * </ul>
 */
public final class GooglyServerCommands {

    /** The {@code /sg} root literal, shared by the server tree, the client tree, and Fabric's routing. */
    public static final String ROOT = "sg";

    /**
     * {@code /sg mob rot} azimuth is measured from +X toward +Z; vanilla yaw 0 faces +Z, so
     * yaw = azimuth - this offset.
     */
    private static final float AZIMUTH_TO_YAW_OFFSET = 90.0F;

    private static final float MAX_MOB_MOVE = 20.0F;
    private static final double MAX_MOB_MOVE_SQUARED = (double) MAX_MOB_MOVE * MAX_MOB_MOVE;

    /** The {@code /sg admin behavior} token that picks a random behavior instead of naming one. */
    private static final String RANDOM_BEHAVIOR_TOKEN = "random";

    /** Suggests the behavior short names plus {@code random} for {@code /sg admin behavior <id>}. */
    private static final SuggestionProvider<CommandSourceStack> BEHAVIOR_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest(RANDOM_BEHAVIOR_TOKEN);
        for (EyeBehavior behavior : EyeBehavior.values()) {
            builder.suggest(behavior.id().getPath());
        }
        return builder.buildFuture();
    };

    private GooglyServerCommands() {
    }

    /** The permission-level-2 {@code admin} subtree, grafted under {@code /sg} by {@link #register}. */
    private static LiteralArgumentBuilder<CommandSourceStack> adminTree() {
        return Commands.literal("admin")
                .requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("eyes")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    LivingEntity target = requireTarget(ctx);
                                    if (target == null) return 0;
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    EyeState.setHasEyes(target, value);
                                    return feedback(ctx, Component.translatable(
                                            "somegoogly.command.admin.has_eyes_set", booleanValue(value)));
                                })))
                .then(Commands.literal("tint")
                        .then(colorBranch("iris", true))
                        .then(colorBranch("cornea", false))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    LivingEntity target = requireTarget(ctx);
                                    if (target == null) return 0;
                                    EyeState.clearTints(target);
                                    return feedback(ctx, Component.translatable("somegoogly.command.admin.tint_cleared"));
                                })))
                .then(Commands.literal("glow")
                        .then(Commands.literal("on").executes(ctx -> glow(ctx, Boolean.TRUE)))
                        .then(Commands.literal("off").executes(ctx -> glow(ctx, Boolean.FALSE)))
                        .then(Commands.literal("config").executes(ctx -> glow(ctx, null))))
                .then(Commands.literal("behavior")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(BEHAVIOR_SUGGESTIONS)
                                .executes(ctx -> behavior(ctx, StringArgumentType.getString(ctx, "id")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> spawnTree(CommandBuildContext buildContext) {
        return Commands.literal("spawn")
                .requires(GooglyServerCommands::pickerUser)
                .then(Commands.argument("type", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                                        .filter(id -> PickerSpawnService.isSpawnable(BuiltInRegistries.ENTITY_TYPE.getValue(id))),
                                builder))
                        .executes(GooglyServerCommands::spawn));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> spawnAllTree() {
        return Commands.literal("spawnall")
                .requires(GooglyServerCommands::pickerUser)
                .executes(ctx -> spawnAll(ctx, null))
                .then(Commands.argument("mod", StringArgumentType.word())
                        .executes(ctx -> spawnAll(ctx, StringArgumentType.getString(ctx, "mod"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mobTree() {
        return Commands.literal("mob")
                .requires(GooglyServerCommands::pickerUser)
                .then(Commands.literal("move")
                        .then(Commands.argument("dx", FloatArgumentType.floatArg(-MAX_MOB_MOVE, MAX_MOB_MOVE))
                                .then(Commands.argument("dy", FloatArgumentType.floatArg(-MAX_MOB_MOVE, MAX_MOB_MOVE))
                                        .then(Commands.argument("dz", FloatArgumentType.floatArg(-MAX_MOB_MOVE, MAX_MOB_MOVE))
                                                .executes(GooglyServerCommands::moveMob)))))
                .then(Commands.literal("rot")
                        .then(Commands.argument("azimuth", FloatArgumentType.floatArg(-360.0F, 360.0F))
                                .executes(GooglyServerCommands::rotateMob)));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PickerSpawnService.spawnOne(player, ResourceArgument.getResource(ctx, "type", Registries.ENTITY_TYPE).value());
        return 1;
    }

    private static int spawnAll(CommandContext<CommandSourceStack> ctx, @Nullable String modFilter)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!ServerConfig.ALLOW_SPAWN_ALL.get()) {
            tell(player, "somegoogly.command.picker.spawnall_disabled");
            return 0;
        }
        if (modFilter != null && !ServerConfig.validateNamespace(modFilter)) {
            ctx.getSource().sendFailure(Component.translatable("somegoogly.command.picker.feedback",
                    Component.translatable("somegoogly.command.picker.spawnall_invalid_mod", modFilter)));
            return 0;
        }
        if (!PickerGate.allowSpawnAll(player.serverLevel().getServer())) {
            tell(player, "somegoogly.command.picker.spawnall_cooldown");
            return 0;
        }
        player.sendSystemMessage(modFilter == null
                ? Component.translatable("somegoogly.command.picker.spawning_all")
                : Component.translatable("somegoogly.command.picker.spawning_mod", modFilter));
        PickerSpawnService.spawn(player, modFilter);
        return 1;
    }

    private static int moveMob(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        double dx = FloatArgumentType.getFloat(ctx, "dx");
        double dy = FloatArgumentType.getFloat(ctx, "dy");
        double dz = FloatArgumentType.getFloat(ctx, "dz");
        if (dx * dx + dy * dy + dz * dz > MAX_MOB_MOVE_SQUARED) {
            tell(player, "somegoogly.command.picker.mob_out_of_range", MAX_MOB_MOVE);
            return 0;
        }
        LivingEntity living = frozenMob(player);
        if (living == null) {
            return 0;
        }
        living.teleportTo(living.getX() + dx, living.getY() + dy, living.getZ() + dz);
        tell(player, "somegoogly.command.picker.mob_moved",
                String.format("%.2f", living.getX()), String.format("%.2f", living.getY()),
                String.format("%.2f", living.getZ()));
        return 1;
    }

    private static int rotateMob(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LivingEntity living = frozenMob(player);
        if (living == null) {
            return 0;
        }
        float azimuth = FloatArgumentType.getFloat(ctx, "azimuth");
        float yaw = Mth.wrapDegrees(azimuth - AZIMUTH_TO_YAW_OFFSET);
        living.setYRot(yaw);
        living.setYHeadRot(yaw);
        living.setYBodyRot(yaw);
        tell(player, "somegoogly.command.picker.mob_rotated", String.format("%.0f", azimuth));
        return 1;
    }

    @Nullable
    private static LivingEntity frozenMob(ServerPlayer player) {
        var mobId = PickerFreezeService.frozenMobId(player.getUUID());
        if (mobId == null) {
            tell(player, "somegoogly.command.picker.mob_not_chosen");
            return null;
        }
        Entity entity = player.serverLevel().getEntity(mobId);
        if (!(entity instanceof LivingEntity living)) {
            tell(player, "somegoogly.command.picker.mob_not_found");
            return null;
        }
        return living;
    }

    private static boolean pickerUser(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && PickerGate.mayUsePicker(player);
    }

    /**
     * Drive the server behavior scheduler by hand: trigger {@code id} (a short name like {@code stare},
     * a full {@code somegoogly:stare}, or {@code random}) on the looked-at mob. Exercises the full
     * server → packet → client play path. Honors the one-at-a-time rule, so it reports if dropped.
     */
    private static int behavior(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        LivingEntity target = requireTarget(ctx);
        if (target == null) return 0;

        EyeBehavior behavior;
        if (id.equalsIgnoreCase(RANDOM_BEHAVIOR_TOKEN)) {
            EyeBehavior[] pool = EyeBehavior.values();
            behavior = pool[target.getRandom().nextInt(pool.length)];
        } else {
            ResourceLocation key = id.indexOf(':') >= 0 ? ResourceLocation.tryParse(id)
                    : ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, id);
            behavior = key == null ? null : EyeBehavior.byId(key);
        }
        if (behavior == null) {
            adminFailure(ctx.getSource(), Component.translatable("somegoogly.command.admin.unknown_behavior", id));
            return 0;
        }

        boolean started = ServerBehaviorScheduler.trigger(
                target, behavior, behavior.defaultDuration(), target.getRandom().nextLong());
        return feedback(ctx, started
                ? Component.translatable("somegoogly.command.admin.behavior_playing", behavior.id().getPath())
                : Component.translatable("somegoogly.command.admin.behavior_dropped", behavior.id().getPath()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> colorBranch(String name, boolean iris) {
        return Commands.literal(name)
                .then(Commands.argument("r", FloatArgumentType.floatArg(0, 1))
                        .then(Commands.argument("g", FloatArgumentType.floatArg(0, 1))
                                .then(Commands.argument("b", FloatArgumentType.floatArg(0, 1))
                                        .executes(ctx -> {
                                            LivingEntity target = requireTarget(ctx);
                                            if (target == null) return 0;
                                            float r = FloatArgumentType.getFloat(ctx, "r");
                                            float g = FloatArgumentType.getFloat(ctx, "g");
                                            float b = FloatArgumentType.getFloat(ctx, "b");
                                            EyeColor color = new EyeColor(r, g, b);
                                            if (iris) {
                                                EyeState.setIrisTint(target, color);
                                            } else {
                                                EyeState.setCorneaTint(target, color);
                                            }
                                            Component targetName = Component.translatable(iris
                                                    ? "somegoogly.value.iris" : "somegoogly.value.cornea");
                                            return feedback(ctx, Component.translatable(
                                                    "somegoogly.command.admin.tint_set", targetName, color.toHex()));
                                        }))));
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, Component message) {
        // Op-gated world mutation: broadcast to other operators and the console, per vanilla convention.
        ctx.getSource().sendSuccess(() -> Component.translatable("somegoogly.command.admin.feedback", message), true);
        return 1;
    }

    private static void adminFailure(CommandSourceStack source, Component message) {
        source.sendFailure(Component.translatable("somegoogly.command.admin.feedback", message));
    }

    /** Picker feedback to {@code player}, under the shared picker prefix. */
    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback",
                Component.translatable(key, args)));
    }

    private static int glow(CommandContext<CommandSourceStack> ctx, @Nullable Boolean value)
            throws CommandSyntaxException {
        LivingEntity target = requireTarget(ctx);
        if (target == null) return 0;
        EyeState.setGlow(target, value);
        Component display = Component.translatable(value == null
                ? "somegoogly.value.config" : value ? "somegoogly.value.on" : "somegoogly.value.off");
        return feedback(ctx, Component.translatable("somegoogly.command.admin.glow_set", display));
    }

    private static Component booleanValue(boolean value) {
        return Component.translatable(value ? "somegoogly.value.true" : "somegoogly.value.false");
    }

    /** Register the server-owned world-mutation branches of {@code /sg}. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal(ROOT)
                .then(adminTree())
                .then(spawnTree(buildContext))
                .then(spawnAllTree())
                .then(mobTree()));
    }

    /**
     * The shared per-execution guard: the sender must be a player <b>in creative mode</b> (the op-level-2
     * gate on the subtree is registration-time only), looking at a living entity. Returns that entity, or
     * {@code null} (with feedback) when it isn't creative or isn't looking at one; a non-player sender
     * gets vanilla's player-required error. Every admin verb calls this first.
     */
    @Nullable
    private static LivingEntity requireTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isCreative()) {
            adminFailure(source, Component.translatable("somegoogly.command.admin.requires_creative"));
            return null;
        }

        LivingEntity target = LookTarget.livingInCrosshair(player, LookTarget.DEFAULT_REACH);
        if (target == null) {
            adminFailure(source, Component.translatable("somegoogly.command.admin.no_target"));
        }
        return target;
    }
}
