package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/**
 * {@link EyeItemService#interact} (the Optometrist right-click harvest) and
 * {@link EyeItemService#selfRemoveWithShears} (sneak + shears on air). The shears-on-kill harvest and
 * the Slimy Eye application verb are covered by {@link SomeGooglyGameTestsLogic} and
 * {@link EligibilityGameTestsLogic}. The loader adapters' protection-mod / PvP gating around these calls
 * stays source-verified — it needs live listener ordering.
 *
 * <p>For the in-game steps below, get Optometrist shears from an Optometrist book in an anvil, or with
 * {@code /give @s shears[enchantments={levels:{"somegoogly:optometrist":1}}]}.
 */
public final class EyeItemServiceGameTestsLogic {

    private EyeItemServiceGameTestsLogic() {
    }

    private static ItemStack optometristShears(GameTestHelper helper) {
        Holder<Enchantment> optometrist = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModContent.OPTOMETRIST);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(optometrist, 1);
        return shears;
    }

    /**
     * Right-click an eyed, configured mob with Optometrist shears: one eye drops, eyes clear, one
     * durability. In game: give a cow eyes and an iris color with {@code /sg admin eyes true} and
     * {@code /sg admin tint iris 0.2 0.6 0.9}, then right-click it in survival with Optometrist shears; its
     * eyes vanish, one Googly Eye with that iris color drops, and the shears lose one durability.
     */
    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.6F, 0.9F);
        EyeState.setHasEyes(cow, true);
        EyeState.setIrisTint(cow, iris);

        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);

        InteractionResult result = EyeItemService.interact(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, cow);

        helper.assertTrue(result == InteractionResult.SUCCESS, "the Optometrist harvest consumes the interaction");
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
            ServerEyeConfigs.replaceAll(Map.of());
            helper.assertTrue(
                    EyeItemService.interact(player, level, InteractionHand.MAIN_HAND, cow) == InteractionResult.PASS,
                    "an unconfigured mob passes rather than dropping a config-less eye");
        } finally {
            ServerEyeConfigs.replaceAll(original);
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
     * Sneak + shears on air removes your own eyes for one durability, whether or not the shears carry
     * Optometrist; only a non-sneaking use is left to the vanilla item. The plain-shears self-damage
     * (a melee hit's worth of health) is not asserted here — fake players are inert to {@code hurt} —
     * and stays source-verified. In game: apply a Slimy Eye to yourself, then sneak and right-click the
     * air with Optometrist shears; your eyes come off, one Googly Eye drops, the shears lose one durability,
     * and you take no damage. Repeat with plain shears and you also lose some health. Without sneaking, or
     * with {@code googlyEyesEnabled = false}, nothing happens.
     */
    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper, Player player) {
        player.setHealth(player.getMaxHealth());
        player.setShiftKeyDown(true);
        EyeState.setHasEyes(player, true);
        EyeState.setIrisTint(player, new EyeColor(0.1F, 0.2F, 0.3F));

        ItemStack opto = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, opto);
        InteractionResult clean = EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(clean == InteractionResult.SUCCESS, "Optometrist self-removal succeeds");
        helper.assertTrue(!EyeState.hasEyes(player), "the player loses their eyes");
        helper.assertTrue(opto.getDamageValue() == 1, "one durability for the clean removal");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(),
                "Optometrist self-removal deals no self-damage");

        ItemStack plain = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        EyeState.setHasEyes(player, true);
        InteractionResult costly = EyeItemService.selfRemoveWithShears(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(costly == InteractionResult.SUCCESS, "plain-shears self-removal still succeeds");
        helper.assertTrue(plain.getDamageValue() == 1, "one durability for the plain removal");
        helper.assertTrue(!EyeState.hasEyes(player), "the plain-shears path also removes the eyes");

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

        EyeState.disableAndClearProperties(player);
        player.setHealth(player.getMaxHealth());
        helper.succeed();
    }
}
