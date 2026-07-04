package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The {@code admin} subtree of {@code /sg} — operator-only (permission level 2) tools that mutate the
 * live {@link LivingEntity} the running player is looking at: its has-eyes flag, iris/cornea tint, glow
 * mode, and active cosmetic behavior. Exercises the full per-mob override loop (server NBT write →
 * {@link EyeState} broadcast → client apply → renderer override) and the server-owned behavior schedule.
 *
 * <p>Server-authoritative: registered on the server dispatcher and grafted under a server-side
 * {@code /sg} root. The client {@code /sg} picker verbs and these admin verbs live on disjoint paths
 * ({@code admin …} vs the picker verbs), so Minecraft/Forge command fall-through routes each side's input
 * to the side that owns it — one command name, two registration sources. Real shears / potion / dye /
 * redstone gameplay calls the same {@link EyeState} API; this is the manual driver for it.
 *
 * <ul>
 *   <li>{@code /sg admin eyes <true|false>} — toggle the has-eyes flag</li>
 *   <li>{@code /sg admin tint iris <r> <g> <b>} / {@code tint cornea <r> <g> <b>} — set a color (0-255)</li>
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
                                    return feedback(ctx, "has-eyes = " + value);
                                })))
                .then(Commands.literal("tint")
                        .then(colorBranch("iris", true))
                        .then(colorBranch("cornea", false))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    LivingEntity target = requireTarget(ctx);
                                    if (target == null) return 0;
                                    EyeState.clearIrisTint(target);
                                    EyeState.clearCorneaTint(target);
                                    return feedback(ctx, "cleared iris + cornea tint");
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
                    : new ResourceLocation("somegoogly", id);
            behavior = key == null ? null : EyeBehaviors.byId(key);
        }
        if (behavior == null) {
            ctx.getSource().sendFailure(Component.literal("[sg admin] unknown behavior '" + id + "'"));
            return 0;
        }

        boolean started = ServerBehaviorScheduler.trigger(
                target, behavior, behavior.defaultDuration(), target.getRandom().nextLong());
        return feedback(ctx, started
                ? "playing " + behavior.id().getPath()
                : "dropped " + behavior.id().getPath() + " (mob is busy)");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> colorBranch(String name, boolean iris) {
        return Commands.literal(name)
                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                        .executes(ctx -> {
                                            LivingEntity target = requireTarget(ctx);
                                            if (target == null) return 0;
                                            int r = IntegerArgumentType.getInteger(ctx, "r");
                                            int g = IntegerArgumentType.getInteger(ctx, "g");
                                            int b = IntegerArgumentType.getInteger(ctx, "b");
                                            EyeColor color = new EyeColor(r / 255F, g / 255F, b / 255F);
                                            if (iris) {
                                                EyeState.setIrisTint(target, color);
                                            } else {
                                                EyeState.setCorneaTint(target, color);
                                            }
                                            return feedback(ctx, name + " tint = #" + String.format("%06X", color.toRgb24()));
                                        }))));
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal("[sg admin] " + message), false);
        return 1;
    }

    private static int glow(CommandContext<CommandSourceStack> ctx, @Nullable Boolean value) {
        LivingEntity target = requireTarget(ctx);
        if (target == null) return 0;
        EyeState.setGlow(target, value);
        return feedback(ctx, "glow = " + (value == null ? "config" : value));
    }

    /** Register the server-side {@code /sg} root carrying the {@code admin} subtree. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sg").then(adminTree()));
    }

    /** The living entity the source player is looking at, or {@code null} (with feedback) if none. */
    @Nullable
    private static LivingEntity requireTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("[sg admin] must be run by a player"));
            return null;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * REACH, look.y * REACH, look.z * REACH);
        AABB search = player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, search,
                e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(),
                REACH * REACH);

        Entity target = hit == null ? null : hit.getEntity();
        if (target instanceof LivingEntity living) {
            return living;
        }
        source.sendFailure(Component.literal("[sg admin] not looking at a living entity"));
        return null;
    }
}
