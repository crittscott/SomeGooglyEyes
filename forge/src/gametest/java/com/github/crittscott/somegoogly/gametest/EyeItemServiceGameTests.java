package com.github.crittscott.somegoogly.gametest;

import java.util.List;
import java.util.ArrayList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.item.EyeItemService;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/**
 * Forge GameTest entry points for {@link EyeItemServiceGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class EyeItemServiceGameTests {

    private EyeItemServiceGameTests() {
    }

    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractHarvestsEyesForOneDurability(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractPassesWhenNotApplicable(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.selfRemoveWithShearsDropsAnEyeAndCostsDurability(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void anotherPlayersEyesFollowThePvpRules(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.anotherPlayersEyesFollowThePvpRules(helper);
    }

    public static void harvestBreakingShearsReportsThem(GameTestHelper helper) {
        ServerPlayer player = SomeGooglyGameTests.survivalPlayer(helper);
        // Creative skips durability.
        player.setGameMode(GameType.SURVIVAL);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeState.setHasEyes(cow, true);
        ItemStack shears = optometristShears(helper);
        shears.setDamageValue(shears.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);

        List<PlayerDestroyItemEvent> reports = new ArrayList<>();
        var listener = PlayerDestroyItemEvent.BUS.addListener(event -> {
            if (event.getEntity() == player) {
                reports.add(event);
            }
        });
        try {
            EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, cow);
        } finally {
            PlayerDestroyItemEvent.BUS.removeListener(listener);
        }

        helper.assertTrue(shears.isEmpty(), Component.literal("the last durability breaks the shears"));
        helper.assertTrue(reports.size() == 1, Component.literal("the broken shears are reported once, got " + reports.size()));
        helper.succeed();
    }

    public static void optometristActsOnArmorStandBeforeItTakesTheShears(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, optometristShears(helper));
        EyeState.setHasEyes(stand, true);

        PlayerInteractEvent.EntityInteractSpecific event =
                new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, stand, Vec3.ZERO);
        boolean canceled = PlayerInteractEvent.EntityInteractSpecific.BUS.post(event);

        helper.assertTrue(canceled, Component.literal("the harvest should end the click before the stand's own interaction"));
        helper.assertTrue(!EyeState.hasEyes(stand), Component.literal("the stand's eyes should come off"));
        helper.succeed();
    }

    private static ItemStack optometristShears(GameTestHelper helper) {
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModContent.OPTOMETRIST), 1);
        return shears;
    }
}
