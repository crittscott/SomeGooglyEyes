package com.github.crittscott.somegoogly.command;

import com.github.crittscott.somegoogly.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.behavior.EyeBehaviors;
import com.github.crittscott.somegoogly.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.state.EyeState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
 * Temporary dev/verification command for Keystone A. Exercises the full per-mob override loop
 * (server NBT write → {@link EyeState} broadcast → client apply → renderer override) against the
 * {@link LivingEntity} the running player is looking at.
 *
 * <p>Not a user-facing feature — the real shears / potion / dye / redstone features will call the
 * same {@link EyeState} API. Op-only.
 *
 * <ul>
 *   <li>{@code /sgdebug eyes <true|false>} — toggle the has-eyes flag</li>
 *   <li>{@code /sgdebug tint iris <r> <g> <b>} / {@code tint cornea <r> <g> <b>} — set a colour (0-255)</li>
 *   <li>{@code /sgdebug tint clear} — drop both colour overrides</li>
 *   <li>{@code /sgdebug glow <on|off|config>} — force glow on/off, or revert to per-eye config</li>
 * </ul>
 */
public final class GooglyDebugCommand {

    private static final double REACH = 20.0;

    private GooglyDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sgdebug")
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
                                .executes(ctx -> behavior(ctx, StringArgumentType.getString(ctx, "id"))))));
    }

    /** Suggests the behaviour short names plus {@code random} for {@code /sgdebug behavior <id>}. */
    private static final SuggestionProvider<CommandSourceStack> BEHAVIOR_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("random");
        for (EyeBehavior behavior : EyeBehaviors.all()) {
            builder.suggest(behavior.id().getPath());
        }
        return builder.buildFuture();
    };

    /**
     * Drive the server behaviour scheduler by hand: trigger {@code id} (a short name like {@code stare},
     * a full {@code somegoogly:stare}, or {@code random}) on the looked-at mob. Exercises the full
     * server → packet → client play path. Honours the one-at-a-time rule, so it reports if dropped.
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
            ctx.getSource().sendFailure(Component.literal("[sgdebug] unknown behavior '" + id + "'"));
            return 0;
        }

        boolean started = ServerBehaviorScheduler.trigger(
                target, behavior, behavior.defaultDuration(), target.getRandom().nextLong());
        return feedback(ctx, started
                ? "playing " + behavior.id().getPath()
                : "dropped " + behavior.id().getPath() + " (mob is busy)");
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> colorBranch(String name, boolean iris) {
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
                                            int rgb = (r << 16) | (g << 8) | b;
                                            if (iris) {
                                                EyeState.setIrisTint(target, rgb);
                                            } else {
                                                EyeState.setCorneaTint(target, rgb);
                                            }
                                            return feedback(ctx, name + " tint = #" + String.format("%06X", rgb));
                                        }))));
    }

    private static int glow(CommandContext<CommandSourceStack> ctx, @Nullable Boolean value) {
        LivingEntity target = requireTarget(ctx);
        if (target == null) return 0;
        EyeState.setGlow(target, value);
        return feedback(ctx, "glow = " + (value == null ? "config" : value));
    }

    /** The living entity the source player is looking at, or {@code null} (with feedback) if none. */
    @Nullable
    private static LivingEntity requireTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("[sgdebug] must be run by a player"));
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
        source.sendFailure(Component.literal("[sgdebug] not looking at a living entity"));
        return null;
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal("[sgdebug] " + message), false);
        return 1;
    }
}
