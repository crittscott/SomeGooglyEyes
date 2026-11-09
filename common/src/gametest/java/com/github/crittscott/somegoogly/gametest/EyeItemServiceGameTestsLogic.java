package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.EyeItemService;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * {@link EyeItemService#interact} (the Optometrist right-click harvest) and
 * {@link EyeItemService#selfRemoveWithShears} (sneak + shears on air). The shears-on-kill harvest and
 * the Slimy Eye application verb are covered by {@link SomeGooglyGameTestsLogic} and
 * {@link EligibilityGameTestsLogic}. The loader adapters' protection-mod ordering around these calls stays
 * source-verified — it needs live listener ordering.
 *
 * <p>For the in-game steps below, get Optometrist shears from an Optometrist book in an anvil, or with
 * {@code /give @s shears[enchantments={levels:{"somegoogly:optometrist":1}}]}.
 */
public final class EyeItemServiceGameTestsLogic {

    private EyeItemServiceGameTestsLogic() {
    }

    /**
     * Eyes go on or come off another player only where you could hurt them. In game, on a server with
     * {@code pvp=false}: right-click another player with a Slimy Eye, then give them eyes with
     * {@code /sg admin eyes true} and right-click them with Optometrist shears; nothing happens either way and
     * nothing is spent. With {@code pvp=true}, put both players on a team with {@code friendlyFire false} and
     * repeat; nothing happens. With {@code friendlyFire true} the shears take the eyes.
     */
    public static void anotherPlayersEyesFollowThePvpRules(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer actor = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "pvp_actor"));
        ServerPlayer victim = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "pvp_victim"));
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        // A player without a connection can't take moveTo, which resets the connection's position.
        victim.setPos(at.x, at.y, at.z);
        ItemStack shears = optometristShears(helper);
        ItemStack slimy = new ItemStack(ModContent.SLIMY_EYE.get());
        Runnable refused = () -> {
            EyeState.setHasEyes(victim, true);
            actor.setItemInHand(InteractionHand.MAIN_HAND, shears);
            helper.assertTrue(EyeItemService.interact(actor, helper.getLevel(), InteractionHand.MAIN_HAND, victim)
                    == InteractionResult.FAIL, "a protected player refuses the harvest");
            helper.assertTrue(EyeState.hasEyes(victim) && shears.getDamageValue() == 0,
                    "a refused harvest changes nothing");
            EyeState.setHasEyes(victim, false);
            actor.setItemInHand(InteractionHand.MAIN_HAND, slimy);
            helper.assertTrue(EyeItemService.interact(actor, helper.getLevel(), InteractionHand.MAIN_HAND, victim)
                    == InteractionResult.FAIL, "a protected player refuses a Slimy Eye");
            helper.assertTrue(!EyeState.hasEyes(victim) && slimy.getCount() == 1,
                    "a refused application changes nothing");
        };

        boolean pvp = server.isPvpAllowed();
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("somegoogly_pvp_test");
        try {
            server.setPvpAllowed(false);
            refused.run();

            server.setPvpAllowed(true);
            scoreboard.addPlayerToTeam(actor.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(victim.getScoreboardName(), team);
            team.setAllowFriendlyFire(false);
            refused.run();

            team.setAllowFriendlyFire(true);
            EyeState.setHasEyes(victim, true);
            actor.setItemInHand(InteractionHand.MAIN_HAND, shears);
            helper.assertTrue(EyeItemService.interact(actor, helper.getLevel(), InteractionHand.MAIN_HAND, victim)
                    == InteractionResult.CONSUME, "a player you could hurt can be harvested");
            helper.assertTrue(!EyeState.hasEyes(victim) && shears.getDamageValue() == 1,
                    "the allowed harvest takes the eyes for one durability");
        } finally {
            server.setPvpAllowed(pvp);
            scoreboard.removePlayerTeam(team);
            droppedEyes(helper, victim).forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    private static ItemStack optometristShears(GameTestHelper helper) {
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(ModContent.OPTOMETRIST.get(), 1);
        return shears;
    }

    private static List<ItemEntity> droppedEyes(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2.0),
                item -> item.getItem().is(ModContent.GOOGLY_EYE.get()));
    }

    /**
     * Right-click an eyed, configured mob with Optometrist shears: one eye drops, eyes clear, one
     * durability. In game: give a cow eyes and an iris color with {@code /sg admin eyes true} and
     * {@code /sg admin tint iris 0.2 0.6 0.9}, then right-click it in survival with Optometrist shears; its
     * eyes vanish, one Googly Eye with that iris color drops, and the shears lose one durability.
     */
    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper, Player player) {
        // The loader's shared fake player takes the server's default game mode, and creative skips both the
        // consumption and the durability cost this test checks.
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.6F, 0.9F);
        EyeState.setHasEyes(cow, true);
        EyeState.setIrisTint(cow, iris);

        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);

        InteractionResult result = EyeItemService.interact(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, cow);

        helper.assertTrue(result == InteractionResult.CONSUME, "the Optometrist harvest consumes the interaction");
        helper.assertTrue(!EyeState.hasEyes(cow), "the harvest clears the mob's eyes");
        helper.assertTrue(shears.getDamageValue() == 1, "a successful harvest spends one shears durability");

        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, cow.getBoundingBox().inflate(4.0));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModContent.GOOGLY_EYE.get()),
                "exactly one Googly Eye is dropped");
        helper.assertTrue(iris.equals(EyeItemProperties.get(drops.get(0).getItem()).iris().orElse(null)),
                "the dropped eye carries the mob's effective iris color");
        helper.succeed();
    }

    /**
     * Every non-qualifying right-click passes to the vanilla interaction and spends nothing. In game: on an
     * eyed sheep, a stick does nothing and plain shears shear its wool as usual; Optometrist shears do
     * nothing to an eyeless cow, nor to an eyed one while {@code googlyEyesEnabled = false}. No eye drops
     * and the Optometrist shears keep full durability.
     */
    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        Level level = helper.getLevel();

        EyeState.setHasEyes(cow, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                "a non-shears item passes");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                "plain shears pass — right-click harvest needs Optometrist");

        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        EyeState.setHasEyes(cow, false);
        helper.assertTrue(EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                "an eyeless mob passes");

        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            EyeState.setHasEyes(cow, true);
            TestEyeConfigs.install(Map.of());
            helper.assertTrue(
                    EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                    "an unconfigured mob passes rather than dropping a config-less eye");
        } finally {
            TestEyeConfigs.install(original);
        }

        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            EyeState.setHasEyes(cow, true);
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(
                    EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                    "googlyEyesEnabled=false passes rather than harvesting");
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
        }

        helper.assertTrue(shears.getDamageValue() == 0, "no passed interaction spends durability");
        helper.assertTrue(EyeState.hasEyes(cow), "no passed interaction removes the eyes");
        helper.succeed();
    }

    /**
     * Loot that rolls enchantments by level with treasure allowed, as fishing treasure and End City chests do,
     * can put Optometrist on shears or a book but on nothing else. In game: run
     * {@code /loot give @s loot minecraft:chests/end_city_treasure} many times; no tool, weapon, or armor piece
     * comes with Optometrist.
     */
    public static void optometristRollsOnlyOntoShearsAndBooks(GameTestHelper helper) {
        int level = 30; // within Optometrist's cost range, as fishing treasure rolls
        helper.assertTrue(rollsOptometrist(level, new ItemStack(Items.SHEARS)), "shears can roll Optometrist");
        helper.assertTrue(rollsOptometrist(level, new ItemStack(Items.BOOK)), "a book can roll Optometrist");
        for (ItemStack other : List.of(new ItemStack(Items.BOW), new ItemStack(Items.FISHING_ROD),
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_CHESTPLATE))) {
            helper.assertTrue(!rollsOptometrist(level, other), other.getItem() + " cannot roll Optometrist");
        }
        helper.succeed();
    }

    private static boolean rollsOptometrist(int level, ItemStack stack) {
        for (EnchantmentInstance result : EnchantmentHelper.getAvailableEnchantmentResults(level, stack, true)) {
            if (result.enchantment == ModContent.OPTOMETRIST.get()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sneak + shears on air removes your own eyes for one durability, whether or not the shears carry
     * Optometrist; only a non-sneaking use is left to the vanilla item. The plain-shears self-damage
     * (a melee hit's worth of health) is not asserted here — fake players are inert to {@code hurt} —
     * and stays source-verified. In game: apply a Slimy Eye to yourself, then sneak and right-click the
     * air with Optometrist shears; your eyes come off, one Googly Eye drops, the shears lose one durability,
     * and you take no damage. Repeat with plain shears and you also lose some health. Without sneaking, or
     * with {@code googlyEyesEnabled = false}, nothing happens.
     * The refusal case has no in-game form without a protection mod; it guards that a loader's denial of
     * item use leaves eyes, drops, durability, and health unchanged.
     * Callback ordering needs a connected client; this service test bypasses the loader callback.
     */
    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper, Player player) {
        // The loader's shared fake player takes the server's default game mode, and creative skips both the
        // consumption and the durability cost this test checks.
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.moveTo(at.x, at.y, at.z);
        player.setHealth(player.getMaxHealth());
        player.setShiftKeyDown(true);
        EyeState.setHasEyes(player, true);
        EyeColor iris = new EyeColor(0.1F, 0.2F, 0.3F);
        EyeState.setIrisTint(player, iris);

        ItemStack opto = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, opto);
        InteractionResult clean = EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(clean == InteractionResult.CONSUME, "Optometrist self-removal succeeds");
        helper.assertTrue(!EyeState.hasEyes(player), "the player loses their eyes");
        helper.assertTrue(opto.getDamageValue() == 1, "one durability for the clean removal");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(),
                "Optometrist self-removal deals no self-damage");
        List<ItemEntity> drops = droppedEyes(helper, player);
        helper.assertTrue(drops.size() == 1, "the clean removal drops one Googly Eye");
        helper.assertTrue(iris.equals(EyeItemProperties.get(drops.get(0).getItem()).iris().orElse(null)),
                "the dropped eye carries the player's iris color");

        ItemStack plain = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        EyeState.setHasEyes(player, true);
        InteractionResult costly = EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(costly == InteractionResult.CONSUME, "plain-shears self-removal still succeeds");
        helper.assertTrue(plain.getDamageValue() == 1, "one durability for the plain removal");
        helper.assertTrue(!EyeState.hasEyes(player), "the plain-shears path also removes the eyes");
        helper.assertTrue(droppedEyes(helper, player).size() == 2, "the plain removal drops a second Googly Eye");

        player.setShiftKeyDown(false);
        EyeState.setHasEyes(player, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(
                EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "self-removal requires sneaking");

        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            player.setShiftKeyDown(true);
            EyeState.setHasEyes(player, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, opto);
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(
                    EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                    "googlyEyesEnabled=false blocks self-removal");
            helper.assertTrue(EyeState.hasEyes(player), "a blocked self-removal leaves the eyes on");
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
        }

        droppedEyes(helper, player).forEach(ItemEntity::discard);
        EyeState.disableAndClearProperties(player);
        player.setHealth(player.getMaxHealth());
        helper.succeed();
    }
}
