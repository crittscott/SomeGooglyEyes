package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.item.EyeItemService;
import net.minecraft.core.BlockPos;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * {@link EyeItemService#interact} (the Optometrist right-click harvest) and
 * {@link EyeItemService#selfRemoveWithShears} (sneak + shears on air). The shears-on-kill harvest and
 * the Slimy Eye application verb are covered by {@link SomeGooglyGameTestsLogic} and
 * {@link EligibilityGameTestsLogic}. {@link LoaderInteractionGameTestsLogic} drives the same uses through each
 * loader's own events or callbacks, including refusal by a protection listener on NeoForge and Forge; Fabric's
 * protection-listener ordering is checked by hand.
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
        ServerPlayer actor = new ServerPlayer(server, helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "pvp_actor"), ClientInformation.createDefault()) {
            @Override
            public boolean isCreative() {
                return false;
            }
        };
        actor.getAbilities().instabuild = false;
        ServerPlayer pvpOff = pvpVictim(helper, "pvp_off");
        ServerPlayer friendly = pvpVictim(helper, "pvp_friendly");
        ServerPlayer allowed = pvpVictim(helper, "pvp_allowed");
        ItemStack shears = optometristShears(helper);
        ItemStack slimy = new ItemStack(ModContent.SLIMY_EYE.get());
        BiConsumer<ServerPlayer, String> refused = (victim, rule) -> {
            EyeState.setHasEyes(victim, true);
            actor.setItemInHand(InteractionHand.MAIN_HAND, shears);
            helper.assertTrue(EyeItemService.interact(actor, helper.getLevel(), InteractionHand.MAIN_HAND, victim)
                    == InteractionResult.FAIL, Component.literal(rule + " blocks the harvest"));
            helper.assertTrue(EyeState.hasEyes(victim) && shears.getDamageValue() == 0,
                    Component.literal("a refused harvest changes nothing"));
            EyeState.setHasEyes(victim, false);
            actor.setItemInHand(InteractionHand.MAIN_HAND, slimy);
            helper.assertTrue(EyeItemService.interact(actor, helper.getLevel(), InteractionHand.MAIN_HAND, victim)
                    == InteractionResult.FAIL, Component.literal(rule + " blocks Slimy Eye application"));
            helper.assertTrue(!EyeState.hasEyes(victim) && slimy.getCount() == 1,
                    Component.literal("a refused application changes nothing"));
        };

        var rules = helper.getLevel().getGameRules();
        boolean pvp = rules.get(GameRules.PVP);
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("somegoogly_pvp_test");
        try {
            rules.set(GameRules.PVP, false, server);
            refused.accept(pvpOff, "PvP disabled");

            rules.set(GameRules.PVP, true, server);
            scoreboard.addPlayerToTeam(actor.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(friendly.getScoreboardName(), team);
            team.setAllowFriendlyFire(false);
            refused.accept(friendly, "Friendly fire disabled");

            EyeState.setHasEyes(allowed, true);
            actor.setItemInHand(InteractionHand.MAIN_HAND, shears);
            helper.assertTrue(EyeItemService.interact(actor, helper.getLevel(), InteractionHand.MAIN_HAND, allowed)
                    == InteractionResult.CONSUME, Component.literal("a player you could hurt can be harvested"));
            helper.assertTrue(!EyeState.hasEyes(allowed), Component.literal("the allowed harvest takes the eyes"));
            helper.assertTrue(shears.getDamageValue() == 1, Component.literal("the harvest spends one durability"));
        } finally {
            rules.set(GameRules.PVP, pvp, server);
            scoreboard.removePlayerTeam(team);
            droppedEyes(helper, allowed).forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    private static ServerPlayer pvpVictim(GameTestHelper helper, String name) {
        var level = helper.getLevel();
        ServerPlayer victim = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name),
                ClientInformation.createDefault());
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        victim.setPos(at.x, at.y, at.z);
        return victim;
    }

    private static ItemStack optometristShears(GameTestHelper helper) {
        Holder<Enchantment> optometrist = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModContent.OPTOMETRIST);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(optometrist, 1);
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
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.6F, 0.9F);
        EyeState.setHasEyes(cow, true);
        EyeState.setIrisTint(cow, iris);

        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);

        InteractionResult result = EyeItemService.interact(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, cow);

        helper.assertTrue(result == InteractionResult.CONSUME, Component.literal("the Optometrist harvest consumes the interaction"));
        helper.assertTrue(!EyeState.hasEyes(cow), Component.literal("the harvest clears the mob's eyes"));
        helper.assertTrue(shears.getDamageValue() == 1, Component.literal("a successful harvest spends one shears durability"));

        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, cow.getBoundingBox().inflate(4.0));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModContent.GOOGLY_EYE.get()),
                Component.literal("exactly one Googly Eye is dropped"));
        helper.assertTrue(iris.equals(EyeItemProperties.get(drops.get(0).getItem()).iris().orElse(null)),
                Component.literal("the dropped eye carries the mob's effective iris color"));
        helper.succeed();
    }

    /**
     * Every non-qualifying right-click passes to the vanilla interaction and spends nothing. In game: on an
     * eyed sheep, a stick does nothing and plain shears shear its wool as usual; Optometrist shears do
     * nothing to an eyeless cow, nor to an eyed one while {@code googlyEyesEnabled = false}. No eye drops
     * and the Optometrist shears keep full durability.
     */
    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 2));
        Level level = helper.getLevel();

        EyeState.setHasEyes(cow, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                Component.literal("a non-shears item passes"));

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                Component.literal("plain shears pass — right-click harvest needs Optometrist"));

        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        EyeState.setHasEyes(cow, false);
        helper.assertTrue(EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                Component.literal("an eyeless mob passes"));

        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            EyeState.setHasEyes(cow, true);
            TestEyeConfigs.install(Map.of());
            helper.assertTrue(
                    EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                    Component.literal("an unconfigured mob passes rather than dropping a config-less eye"));
        } finally {
            TestEyeConfigs.install(original);
        }

        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            EyeState.setHasEyes(cow, true);
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(
                    EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                    Component.literal("googlyEyesEnabled=false passes rather than harvesting"));
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
        }

        helper.assertTrue(shears.getDamageValue() == 0, Component.literal("no passed interaction spends durability"));
        helper.assertTrue(EyeState.hasEyes(cow), Component.literal("no passed interaction removes the eyes"));
        helper.succeed();
    }

    /**
     * Loot that rolls enchantments by level from the random-loot set can put Optometrist on shears or a book but on
     * nothing else. In game: run {@code /loot give @s loot minecraft:chests/end_city_treasure} many times; no tool,
     * weapon, or armor piece comes with Optometrist.
     */
    public static void optometristRollsOnlyOntoShearsAndBooks(GameTestHelper helper) {
        int level = 5; // within optometrist.json's min_cost..max_cost, so only the item decides
        helper.assertTrue(rollsOptometrist(helper, level, new ItemStack(Items.SHEARS)),
                Component.literal("shears can roll Optometrist"));
        helper.assertTrue(rollsOptometrist(helper, level, new ItemStack(Items.BOOK)),
                Component.literal("a book can roll Optometrist"));
        for (ItemStack other : List.of(new ItemStack(Items.BOW), new ItemStack(Items.FISHING_ROD),
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_CHESTPLATE))) {
            helper.assertTrue(!rollsOptometrist(helper, level, other),
                    Component.literal(other.getItem() + " cannot roll Optometrist"));
        }
        helper.succeed();
    }

    private static boolean rollsOptometrist(GameTestHelper helper, int level, ItemStack stack) {
        TagKey<Enchantment> randomLoot = TagKey.create(Registries.ENCHANTMENT,
                Identifier.withDefaultNamespace("on_random_loot"));
        Stream<Holder<Enchantment>> possible = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(randomLoot).stream();
        for (EnchantmentInstance result : EnchantmentHelper.getAvailableEnchantmentResults(level, stack, possible)) {
            if (result.enchantment().is(ModContent.OPTOMETRIST)) {
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
     * A protection listener's refusal is checked through the loader's hook by
     * {@link LoaderInteractionGameTestsLogic#useHooksHonorResults}. Callback ordering needs a connected client;
     * this service test bypasses the loader callback.
     */
    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper, Player player) {
        // The loader's shared fake player takes the server's default game mode, and creative skips both the
        // consumption and the durability cost this test checks.
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.snapTo(at.x, at.y, at.z);
        player.setHealth(player.getMaxHealth());
        player.setShiftKeyDown(true);
        EyeState.setHasEyes(player, true);
        EyeColor iris = new EyeColor(0.1F, 0.2F, 0.3F);
        EyeState.setIrisTint(player, iris);

        ItemStack opto = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, opto);
        InteractionResult clean = EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(clean == InteractionResult.CONSUME, Component.literal("Optometrist self-removal succeeds"));
        helper.assertTrue(!EyeState.hasEyes(player), Component.literal("the player loses their eyes"));
        helper.assertTrue(opto.getDamageValue() == 1, Component.literal("one durability for the clean removal"));
        helper.assertTrue(player.getHealth() == player.getMaxHealth(),
                Component.literal("Optometrist self-removal deals no self-damage"));
        List<ItemEntity> drops = droppedEyes(helper, player);
        helper.assertTrue(drops.size() == 1, Component.literal("the clean removal drops one Googly Eye"));
        helper.assertTrue(iris.equals(EyeItemProperties.get(drops.get(0).getItem()).iris().orElse(null)),
                Component.literal("the dropped eye carries the player's iris color"));

        ItemStack plain = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        EyeState.setHasEyes(player, true);
        InteractionResult costly = EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(costly == InteractionResult.CONSUME, Component.literal("plain-shears self-removal still succeeds"));
        helper.assertTrue(plain.getDamageValue() == 1, Component.literal("one durability for the plain removal"));
        helper.assertTrue(!EyeState.hasEyes(player), Component.literal("the plain-shears path also removes the eyes"));
        helper.assertTrue(droppedEyes(helper, player).size() == 2, Component.literal("the plain removal drops a second Googly Eye"));

        player.setShiftKeyDown(false);
        EyeState.setHasEyes(player, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(
                EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                Component.literal("self-removal requires sneaking"));

        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            player.setShiftKeyDown(true);
            EyeState.setHasEyes(player, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, opto);
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(
                    EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                    Component.literal("googlyEyesEnabled=false blocks self-removal"));
            helper.assertTrue(EyeState.hasEyes(player), Component.literal("a blocked self-removal leaves the eyes on"));
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
            // The loader's fake player is shared, and a later test's click would still be sneaking.
            player.setShiftKeyDown(false);
        }

        droppedEyes(helper, player).forEach(ItemEntity::discard);
        EyeState.disableAndClearProperties(player);
        player.setHealth(player.getMaxHealth());
        helper.succeed();
    }
}
