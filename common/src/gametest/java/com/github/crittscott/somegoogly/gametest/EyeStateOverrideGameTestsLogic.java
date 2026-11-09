package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

/** Dispatcher coverage for the visual admin commands. Aim at an eyed cow as a creative operator in game. */
public final class EyeStateOverrideGameTestsLogic {
    private EyeStateOverrideGameTestsLogic() { }

    /** In game: set iris and cornea tint and glow, clear tint, then use glow config. Glow survives tint clear. */
    public static void adminAppearanceCommands(GameTestHelper helper, ServerPlayer template) {
        ServerPlayer player = TestPlayers.create(template, true, 2);
        Cow cow = target(helper, player);
        try {
            helper.assertTrue(command(player, "tint iris 1 0 0") > 0 && command(player, "tint cornea 0 0 1") > 0
                && command(player, "glow on") > 0, "the dispatcher accepts appearance commands");
            helper.assertTrue(EyeState.readProperties(cow).iris().orElseThrow().r() == 1F
                && EyeState.readProperties(cow).cornea().orElseThrow().b() == 1F
                && EyeState.readProperties(cow).glow().orElse(false),
                "the looked-at cow receives the independently chosen red iris");
            helper.assertTrue(command(player, "tint clear") > 0 && EyeState.readProperties(cow).iris().isEmpty()
                && EyeState.readProperties(cow).cornea().isEmpty() && EyeState.readProperties(cow).glow().orElse(false),
                "tint clear preserves glow");
            helper.assertTrue(command(player, "glow config") > 0 && EyeState.readProperties(cow).isEmpty(),
                "glow config removes the last override");
        } finally { cow.discard(); player.discard(); }
        helper.succeed();
    }

    /** In game: try admin commands in survival, without op, with no target, and with invalid color arguments. */
    public static void adminCommandsRefuseUnauthorizedOrInvalidUse(GameTestHelper helper, ServerPlayer template) {
        for (boolean creative : new boolean[]{false, true}) {
            ServerPlayer player = TestPlayers.create(template, creative, creative ? 0 : 2);
            Cow cow = target(helper, player);
            var before = EyeState.snapshot(cow);
            try {
                helper.assertTrue(command(player, "eyes false") == 0 && EyeState.snapshot(cow).equals(before),
                    "source permission alone does not bypass player authorization");
            } finally { cow.discard(); player.discard(); }
        }
        ServerPlayer player = TestPlayers.create(template, true, 2);
        Cow cow = target(helper, player);
        try {
            var before = EyeState.snapshot(cow);
            helper.assertTrue(command(player, "tint iris nope 0 0") == 0 && EyeState.snapshot(cow).equals(before),
                "bad arguments leave state intact");
            cow.discard();
            helper.assertTrue(command(player, "eyes false") == 0, "no looked-at target produces a failure result");
        } finally { player.discard(); }
        helper.succeed();
    }

    /** In game: eyes false/true changes the cow, then behavior swirl starts and a second behavior is dropped. */
    public static void adminEyesAndBehaviorCommands(GameTestHelper helper, ServerPlayer template) {
        ServerPlayer player = TestPlayers.create(template, true, 2);
        Cow cow = target(helper, player);
        ServerBehaviorScheduler.clear();
        try {
            helper.assertTrue(command(player, "eyes false") > 0 && !EyeState.hasEyes(cow), "eyes false reaches the target");
            helper.assertTrue(command(player, "eyes true") > 0 && EyeState.hasEyes(cow), "eyes true restores eyes");
            helper.assertTrue(command(player, "behavior swirl") > 0, "the command starts a swirl");
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 42L), "the commanded swirl owns the schedule");
            command(player, "behavior blink");
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.STARE, 43L), "another command cannot interrupt it");
        } finally { ServerBehaviorScheduler.clear(); cow.discard(); player.discard(); }
        helper.succeed();
    }

    private static int command(ServerPlayer player, String text) {
        try {
            return player.serverLevel().getServer().getCommands().getDispatcher().execute("sg admin " + text,
                player.createCommandSourceStack().withPermission(2).withSuppressedOutput());
        } catch (CommandSyntaxException refusal) {
            return 0;
        }
    }

    private static Cow target(GameTestHelper helper, ServerPlayer player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 4));
        player.moveTo(helper.absolutePos(new BlockPos(2, 2, 1)), 0F, 0F);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, cow.getEyePosition());
        EyeState.setHasEyes(cow, true);
        return cow;
    }
}
