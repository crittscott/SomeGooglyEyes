package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Twilight Forest-only head scenarios: trophies and skull candles taking, keeping, and giving up eyes. They need
 * Twilight Forest loaded.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TwilightHeadGameTests {
    private static final BlockPos HEAD = new BlockPos(1, 2, 1);
    private static final Snapshot EYES = new Snapshot(true, 0.42F, AppearanceOverride.EMPTY);

    private TwilightHeadGameTests() {
    }

    /**
     * In game, give a skeleton skull eyes, right-click it with a red candle, and break it.
     * The skull with a candle and the dropped vanilla skull should show the same eyes.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void playerConversionKeepsEyes(GameTestHelper helper) {
        requireTwilightForest(helper);
        Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
        eyedSkull(helper);
        use(helper, player, new ItemStack(Items.RED_CANDLE));
        checkCandle(helper, "skeleton_skull_candle");

        BlockEntity candle = helper.getBlockEntity(HEAD);
        BlockEntity reloaded = reload(helper, candle);
        helper.assertTrue(EYES.equals(SkullEyeState.snapshot(reloaded)), "candle save keeps eyes");
        breakAndReplace(helper, Items.SKELETON_SKULL, Blocks.SKELETON_SKULL, EYES);
        helper.succeed();
    }

    /**
     * In game, point a dispenser with a red candle at an eyed wall-mounted skeleton skull and power it.
     * The replacement skull candle should keep the eyes.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dispenserConversionKeepsEyes(GameTestHelper helper) {
        requireTwilightForest(helper);
        helper.setBlock(HEAD.south(), Blocks.STONE);
        helper.setBlock(HEAD, Blocks.SKELETON_WALL_SKULL.defaultBlockState()
                .setValue(WallSkullBlock.FACING, Direction.NORTH));
        SkullEyeState.set(helper.getBlockEntity(HEAD), EYES);
        BlockPos dispenserPos = HEAD.above();
        helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.DOWN));
        DispenserBlockEntity dispenser = (DispenserBlockEntity) helper.getBlockEntity(dispenserPos);
        dispenser.setItem(0, new ItemStack(Items.RED_CANDLE));
        helper.setBlock(dispenserPos.above(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> checkCandle(helper, "skeleton_wall_skull_candle"));
    }

    /**
     * In game, apply a Slimy Eye to a Lich trophy, break and replace the trophy, then shear it with
     * Optometrist shears. A second client should see the same eyes until they are sheared.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void lichTrophyTakesKeepsAndGivesUpEyes(GameTestHelper helper) {
        requireTwilightForest(helper);
        Block trophy = twilightBlock("lich_trophy");
        helper.setBlock(HEAD, trophy);
        Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
        use(helper, player, new ItemStack(ModContent.SLIMY_EYE.get()));
        BlockEntity placed = helper.getBlockEntity(HEAD);
        Snapshot eyes = SkullEyeState.snapshot(placed);
        helper.assertTrue(eyes.hasEyes(), "a Slimy Eye gives the trophy eyes");
        helper.assertTrue(placed.getUpdatePacket() != null, "the trophy sends live updates");
        var update = placed.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(eyes.equals(Snapshot.read(update.getCompound(SkullEyeState.DATA_KEY))), "the trophy update tag keeps eyes");
        BlockEntity reloaded = reload(helper, placed);
        helper.assertTrue(eyes.equals(SkullEyeState.snapshot(reloaded)), "the trophy save keeps eyes");

        breakAndReplace(helper, trophy.asItem(), trophy, eyes);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModContent.OPTOMETRIST), 1);
        use(helper, player, shears);
        helper.assertTrue(!SkullEyeState.hasEyes(helper.getBlockEntity(HEAD)), "shearing removes the trophy eyes");
        helper.succeed();
    }

    private static BlockEntity reload(GameTestHelper helper, BlockEntity head) {
        var registries = helper.getLevel().registryAccess();
        return BlockEntity.loadStatic(head.getBlockPos(), head.getBlockState(),
                head.saveWithFullMetadata(registries), registries);
    }

    private static void requireTwilightForest(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("twilightforest"), "Twilight Forest is missing from the GameTest run");
    }

    private static Block twilightBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("twilightforest", path));
    }

    private static void eyedSkull(GameTestHelper helper) {
        helper.setBlock(HEAD, Blocks.SKELETON_SKULL);
        SkullEyeState.set(helper.getBlockEntity(HEAD), EYES);
    }

    private static void use(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(HEAD);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit));
    }

    private static void checkCandle(GameTestHelper helper, String blockId) {
        BlockEntity candle = helper.getBlockEntity(HEAD);
        helper.assertTrue(candle != null && candle.getBlockState().is(twilightBlock(blockId)),
                "the skull was converted to a candle");
        helper.assertTrue(EYES.equals(SkullEyeState.snapshot(candle)), "conversion keeps the eye snapshot");
        var tag = candle.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(EYES.equals(Snapshot.read(tag.getCompound(SkullEyeState.DATA_KEY))),
                "the candle update includes the original eyes");
    }

    private static void breakAndReplace(GameTestHelper helper, Item drop, Block replacement,
                                        Snapshot eyes) {
        BlockPos pos = helper.absolutePos(HEAD);
        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                item -> item.getItem().is(drop));
        helper.assertTrue(drops.size() == 1, "breaking the head drops one head item");
        ItemStack stack = drops.get(0).getItem();
        helper.assertTrue(eyes.equals(SkullEyeState.itemSnapshot(stack)), "the head item keeps the original eyes");
        helper.setBlock(HEAD, replacement);
        helper.getBlockEntity(HEAD).applyComponentsFromItemStack(stack);
        helper.assertTrue(eyes.equals(SkullEyeState.snapshot(helper.getBlockEntity(HEAD))), "placement restores the eyes");
    }
}
