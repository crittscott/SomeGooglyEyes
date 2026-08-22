package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.util.LookTarget;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * The {@code admin} subtree of {@code /sg} — operator (permission level 2) tools, additionally
 * requiring <b>creative mode</b> like the rest of the picker toolset, that mutate the live
 * {@link LivingEntity} the running player is looking at: its has-eyes flag, iris/cornea tint, glow
 * mode, and active cosmetic behavior. Exercises the full per-mob override loop (server NBT write →
 * {@link EyeState} broadcast → client apply → renderer override) and the server-owned behavior schedule.
 *
 * <p>Server-authoritative: registered on the server dispatcher and grafted under a server-side
 * {@code /sg} root. The client {@code /sg} picker verbs and these admin verbs live on disjoint paths
 * ({@code admin …} vs the picker verbs), so Minecraft/Forge command fall-through routes each side's input
 * to the side that owns it — one command name, two registration sources. Real shears / slimy eye / dye /
 * redstone gameplay calls the same {@link EyeState} API; this is the manual driver for it.
 *
 * <ul>
 *   <li>{@code /sg admin eyes <true|false>} — toggle the has-eyes flag</li>
 *   <li>{@code /sg admin tint iris <r> <g> <b>} / {@code tint cornea <r> <g> <b>} — set a color (0–1)</li>
 *   <li>{@code /sg admin tint clear} — drop both color overrides</li>
 *   <li>{@code /sg admin glow <on|off|config>} — force glow on/off, or revert to per-eye config</li>
 *   <li>{@code /sg admin behavior <id|random>} — trigger a cosmetic behavior now</li>
 * </ul>
 */
public final class GooglyAdminCommand {

    /** Suggests the behavior short names plus {@code random} for {@code /sg admin behavior <id>}. */
    private static final SuggestionProvider<CommandSourceStack> BEHAVIOR_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("random");
        for (EyeBehavior behavior : EyeBehaviors.all()) {
            builder.suggest(behavior.id().getPath());
        }
        return builder.buildFuture();
    };

    private static final double REACH = 20.0;

    private GooglyAdminCommand() {
    }

    /** The {@code admin} subtree (op-gated), grafted under {@code /sg} by {@link #register}. */
    private static LiteralArgumentBuilder<CommandSourceStack> adminTree() {
        return Commands.literal("admin")
                .requires(src -> src.hasPermission(2))
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

    /**
     * Drive the server behavior scheduler by hand: trigger {@code id} (a short name like {@code stare},
     * a full {@code somegoogly:stare}, or {@code random}) on the looked-at mob. Exercises the full
     * server → packet → client play path. Honors the one-at-a-time rule, so it reports if dropped.
     */
    private static int behavior(CommandContext<CommandSourceStack> ctx, String id) {
        LivingEntity target = requireTarget(ctx);
        if (target == null) return 0;

        EyeBehavior behavior;
        if (id.equalsIgnoreCase("random")) {
            var pool = EyeBehaviors.all();
            behavior = pool.get(target.getRandom().nextInt(pool.size()));
        } else {
            ResourceLocation key = id.indexOf(':') >= 0 ? ResourceLocation.tryParse(id)
                    : new ResourceLocation(SomeGooglyCommon.MOD_ID, id);
            behavior = key == null ? null : EyeBehaviors.byId(key);
        }
        if (behavior == null) {
            ctx.getSource().sendFailure(Component.translatable("somegoogly.command.admin.unknown_behavior", id));
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
                                                    "somegoogly.command.admin.tint_set", targetName,
                                                    String.format("%06X", color.toRgb24())));
                                        }))));
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> Component.translatable("somegoogly.command.admin.feedback", message), false);
        return 1;
    }

    private static int glow(CommandContext<CommandSourceStack> ctx, @Nullable Boolean value) {
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

    /** Register the server-side {@code /sg} root carrying the {@code admin} subtree. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sg").then(adminTree()));
    }

    /**
     * The shared per-execution guard: the sender must be a player <b>in creative mode</b> (the op-level-2
     * gate on the subtree is registration-time only), looking at a living entity. Returns that entity, or
     * {@code null} (with feedback) when any of that fails. Every admin verb calls this first.
     */
    @Nullable
    private static LivingEntity requireTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("somegoogly.command.admin.not_a_player"));
            return null;
        }
        if (!player.isCreative()) {
            source.sendFailure(Component.translatable("somegoogly.command.admin.requires_creative"));
            return null;
        }

        LivingEntity target = LookTarget.livingInCrosshair(player, REACH);
        if (target == null) {
            source.sendFailure(Component.translatable("somegoogly.command.admin.no_target"));
        }
        return target;
    }
}
