package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.item.EyeItemService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Forge GameTest entry points for {@link EyeItemServiceGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EyeItemServiceGameTests {

    private static final String TEMPLATE = "empty";

    private EyeItemServiceGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void anotherPlayersEyesFollowThePvpRules(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.anotherPlayersEyesFollowThePvpRules(helper);
    }

    /**
     * Shears that break on a harvest are reported as destroyed. In game: with a mod that refills a broken tool
     * from your inventory, harvest an eyed cow with Optometrist shears on their last durability; the mod refills
     * your hand as it would after vanilla shearing.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void harvestBreakingShearsReportsThem(GameTestHelper helper) {
        ServerPlayer player = SomeGooglyGameTests.survivalPlayer(helper);
        // The loader's shared fake player takes the server's default game mode, and creative skips durability.
        player.setGameMode(GameType.SURVIVAL);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeState.setHasEyes(cow, true);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(ModContent.OPTOMETRIST.get(), 1);
        shears.setDamageValue(shears.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);

        List<PlayerDestroyItemEvent> reports = new ArrayList<>();
        Consumer<PlayerDestroyItemEvent> listener = reports::add;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerDestroyItemEvent.class, listener);
        try {
            EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, cow);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }

        helper.assertTrue(shears.isEmpty(), "the last durability breaks the shears");
        helper.assertTrue(reports.size() == 1 && reports.get(0).getOriginal().is(Items.SHEARS)
                        && reports.get(0).getHand() == InteractionHand.MAIN_HAND,
                "the broken shears are reported once, with the hand holding them");
        helper.succeed();
    }

    /**
     * Optometrist shears act on an armor stand's eyes before the stand can take them into its hand. In game: give
     * an armor stand with arms eyes using a Slimy Eye, and right-click it with Optometrist shears; its eyes come
     * off and the shears stay in your hand.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void optometristActsOnArmorStandBeforeItTakesTheShears(GameTestHelper helper) {
        ServerPlayer player = SomeGooglyGameTests.survivalPlayer(helper);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 2));
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(ModContent.OPTOMETRIST.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        EyeState.setHasEyes(stand, true);

        PlayerInteractEvent.EntityInteractSpecific event =
                new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, stand, Vec3.ZERO);
        MinecraftForge.EVENT_BUS.post(event);

        helper.assertTrue(event.isCanceled(), "the harvest should end the click before the stand's own interaction");
        helper.assertTrue(!EyeState.hasEyes(stand), "the stand's eyes should come off");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void optometristInteractHarvestsEyesForOneDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractHarvestsEyesForOneDurability(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void optometristInteractPassesWhenNotApplicable(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.optometristInteractPassesWhenNotApplicable(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void selfRemoveWithShearsDropsAnEyeAndCostsDurability(GameTestHelper helper) {
        EyeItemServiceGameTestsLogic.selfRemoveWithShearsDropsAnEyeAndCostsDurability(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
    }
}
