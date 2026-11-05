package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
