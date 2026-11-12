package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;

/**
 * The visual {@code /sg admin} commands, run through the server's command dispatcher by a player looking at an eyed
 * cow. In game: as a creative operator, look at an eyed cow and run the commands each test names.
 */
public final class EyeStateOverrideGameTestsLogic {

    private EyeStateOverrideGameTestsLogic() {
    }

    /**
     * Tint and glow commands set the looked-at mob's appearance; {@code tint clear} drops both tints but keeps the
     * glow, and {@code glow config} then drops the last override. In game: run {@code /sg admin tint iris 1 0 0},
     * {@code /sg admin tint cornea 0 0 1}, and {@code /sg admin glow on}; the eyes turn red and blue and glow. Run
     * {@code /sg admin tint clear}; the colors return to the definition's but the glow stays. Run
     * {@code /sg admin glow config}; the eyes look as the definition says.
     */
    public static void adminAppearanceCommands(GameTestHelper helper, ServerPlayer template) {
        ServerPlayer player = TestPlayers.create(template, true, TestPlayers.GAMEMASTER);
        Cow cow = target(helper, player);
        try {
            helper.assertTrue(command(player, "tint iris 1 0 0") > 0 && command(player, "tint cornea 0 0 1") > 0
                    && command(player, "glow on") > 0, Component.literal("the dispatcher accepts the appearance commands"));
            AppearanceOverride set = EyeState.readProperties(cow);
            helper.assertTrue(set.iris().orElseThrow().r() == 1F && set.cornea().orElseThrow().b() == 1F
                    && set.glow().orElse(false), Component.literal("the looked-at cow takes the chosen iris, cornea, and glow"));
            AppearanceOverride cleared = command(player, "tint clear") > 0 ? EyeState.readProperties(cow) : null;
            helper.assertTrue(cleared != null && cleared.iris().isEmpty() && cleared.cornea().isEmpty()
                    && cleared.glow().orElse(false), Component.literal("tint clear keeps the glow"));
            helper.assertTrue(command(player, "glow config") > 0 && EyeState.readProperties(cow).isEmpty(),
                    Component.literal("glow config removes the last override"));
        } finally {
            cow.discard();
            player.discard();
        }
        helper.succeed();
    }

    /**
     * Admin commands refuse a survival operator, a creative non-operator, invalid arguments, and no looked-at target,
     * changing nothing. In game: try {@code /sg admin eyes false} while looking at an eyed cow in survival, then as a
     * creative player without operator status; then, as a creative operator, try {@code /sg admin tint iris nope 0 0}
     * and {@code /sg admin eyes false} looking at nothing. Each fails and the cow keeps its eyes.
     */
    public static void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper, ServerPlayer template) {
        for (boolean creative : new boolean[]{false, true}) {
            ServerPlayer player = TestPlayers.create(template, creative, creative ? TestPlayers.NO_PERMISSIONS : TestPlayers.GAMEMASTER);
            Cow cow = target(helper, player);
            EyeState.Snapshot before = EyeState.snapshot(cow);
            try {
                helper.assertTrue(command(player, "eyes false") == 0 && EyeState.snapshot(cow).equals(before),
                        Component.literal("source permission alone does not bypass the player's own authorization"));
            } finally {
                cow.discard();
                player.discard();
            }
        }
        ServerPlayer player = TestPlayers.create(template, true, TestPlayers.GAMEMASTER);
        Cow cow = target(helper, player);
        try {
            EyeState.Snapshot before = EyeState.snapshot(cow);
            helper.assertTrue(command(player, "tint iris nope 0 0") == 0 && EyeState.snapshot(cow).equals(before),
                    Component.literal("bad arguments leave the target unchanged"));
            cow.discard();
            helper.assertTrue(command(player, "eyes false") == 0, Component.literal("no looked-at target fails"));
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    /**
     * {@code eyes false}/{@code eyes true} take and give the looked-at mob's eyes, and {@code behavior swirl} starts a
     * swirl that a second behavior command can't interrupt. In game: run {@code /sg admin eyes false} and
     * {@code /sg admin eyes true} at an eyed cow, then {@code /sg admin behavior swirl} and, while it swirls,
     * {@code /sg admin behavior blink}; the blink is reported dropped.
     */
    public static void adminEyesAndBehaviorCommands(GameTestHelper helper, ServerPlayer template) {
        ServerPlayer player = TestPlayers.create(template, true, TestPlayers.GAMEMASTER);
        Cow cow = target(helper, player);
        ServerBehaviorScheduler.clear();
        try {
            helper.assertTrue(command(player, "eyes false") > 0 && !EyeState.hasEyes(cow), Component.literal("eyes false reaches the target"));
            helper.assertTrue(command(player, "eyes true") > 0 && EyeState.hasEyes(cow), Component.literal("eyes true restores the eyes"));
            helper.assertTrue(command(player, "behavior swirl") > 0, Component.literal("the command starts a swirl"));
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 42L),
                    Component.literal("the commanded swirl occupies the schedule"));
            command(player, "behavior blink");
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.STARE, 43L),
                    Component.literal("another command cannot interrupt it"));
        } finally {
            ServerBehaviorScheduler.clear();
            cow.discard();
            player.discard();
        }
        helper.succeed();
    }

    /** Run {@code /sg admin <text>} as {@code player}; the command's result, or 0 when it doesn't parse. */
    private static int command(ServerPlayer player, String text) {
        try {
            return player.level().getServer().getCommands().getDispatcher().execute("sg admin " + text,
                    player.createCommandSourceStack().withPermission(TestPlayers.GAMEMASTER).withSuppressedOutput());
        } catch (CommandSyntaxException refusal) {
            return 0;
        }
    }

    /** An eyed cow in front of {@code player}, who is turned to look at it. */
    private static Cow target(GameTestHelper helper, ServerPlayer player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 4));
        player.snapTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0F, 0F);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, cow.getEyePosition());
        EyeState.setHasEyes(cow, true);
        return cow;
    }
}
