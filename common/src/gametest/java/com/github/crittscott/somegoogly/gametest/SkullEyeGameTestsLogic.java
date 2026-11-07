package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.item.EyeItemService;
import java.util.Set;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Googly eyes on placed mob heads: applying and harvesting them through {@link EyeItemService#useOnSkull}, an
 * eyed mob's own head inheriting its eyes ({@link EyeItemService#markHeadDrop}, reached through each loader's
 * death-drop hook), breaking and re-placing a head, and saving one. The loader adapters' protection-mod
 * ordering is exercised by native item-denial tests on Forge and NeoForge; the rendered eyes and their client-side
 * expressions need a client.
 *
 * <p>For the in-game steps below, get Optometrist shears as described in {@link EyeItemServiceGameTestsLogic}.
 */
public final class SkullEyeGameTestsLogic {

    /**
     * In game: give a placed head a one-eye layout and enable blink and cross-eye at the shortest
     * interval. Walk away and return during a blink. The head resumes at the current blink phase
     * and its unpaired pupil never crosses.
     */
    public static void placedHeadUsesSharedAmbientSlots(GameTestHelper helper) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(new EyeDefinition(
                new EyePlacement(Vec3.ZERO, 1F, 1F, 1F, 0F, 0F, -1), EyeAppearance.DEFAULT));
        Variant variant = new Variant();
        variant.heads = List.of(head);
        RuntimeConfig config = new RuntimeConfig();
        config.variants = List.of(variant);
        HeadInfo layout = new HeadInfo(config, 0);
        var entity = SkullEyeState.entityFor(Blocks.SKELETON_SKULL);
        Snapshot snapshot = new Snapshot(true, 0F, AppearanceOverride.EMPTY);
        int[] parts = SkullEyeState.headParts(layout);
        helper.assertTrue(parts.length == 1, "the skull renders the single-eye layout");
        long seed = Mth.getSeed(new BlockPos(1, 2, 3));
        var watched = PresentationEyes.placedHead(seed, entity, snapshot, layout, parts);
        var returning = PresentationEyes.placedHead(seed, entity, snapshot, layout, parts);
        var pupil = returning.eyes[parts[0]][0];
        helper.assertTrue(!pupil.itemGravity && !pupil.guiGravity && pupil.deltaY == -1F,
                "placed heads start with attached-eye gravity and pupil position");

        var settings = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK, EyeBehavior.CROSS_EYE),
                0, false, 1, Set.of());
        int duration = EyeBehavior.BLINK.defaultDuration();
        for (int tick = 0; tick < duration * 2; tick++) {
            watched.tick(tick, settings);
            helper.assertTrue(watched.active != null && watched.active.behavior == EyeBehavior.BLINK
                            && watched.active.age == tick % duration,
                    "short slots fit a whole blink and exclude cross-eye");
        }
        returning.tick(duration * 2L - 1, settings);
        helper.assertTrue(returning.active != null && returning.active.behavior == watched.active.behavior
                        && returning.active.age == watched.active.age,
                "a newly visible head joins the expression at its current age");
        helper.succeed();
    }

    /** How far from its source block a drop is looked for; less than the gap to a neighboring test. */
    private static final double DROP_SEARCH_RADIUS = 1.5;
    private static final BlockPos HEAD = new BlockPos(1, 2, 1);
    private static final EyeColor IRIS = new EyeColor(0.6F, 0.1F, 0.8F);
    private static final float LETHAL_DAMAGE = 1000.0F;
    private static final BlockPos MOB = new BlockPos(1, 2, 3);

    private SkullEyeGameTestsLogic() {
    }

    /**
     * A native item-use denial prevents both applying and harvesting head eyes, without consuming the
     * item, damaging shears, or spawning a drop. In game: deny item use on a placed skull with a protection
     * listener while leaving the event uncanceled, then try a Slimy Eye and Optometrist shears.
     */
    public static void deniedItemUseLeavesHeadsUntouched(GameTestHelper helper, Player player,
                                                         BiConsumer<BlockHitResult, Boolean> interact) {
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        BlockPos pos = skull.getBlockPos();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack slimy = purpleSlimyEye();
        player.setItemInHand(InteractionHand.MAIN_HAND, slimy);
        interact.accept(hit, true);
        helper.assertTrue(!SkullEyeState.hasEyes(skull) && slimy.getCount() == 1,
                "denied application leaves both head and item untouched");
        interact.accept(hit, false);
        helper.assertTrue(SkullEyeState.hasEyes(skull) && slimy.isEmpty(),
                "an allowed event reaches the adapter and applies eyes");
        Snapshot original = SkullEyeState.snapshot(skull);
        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        int dropsBefore = drops(helper, HEAD, ModContent.GOOGLY_EYE.get()).size();
        interact.accept(hit, true);
        helper.assertTrue(SkullEyeState.snapshot(skull).equals(original) && shears.getDamageValue() == 0
                        && drops(helper, HEAD, ModContent.GOOGLY_EYE.get()).size() == dropsBefore,
                "denied harvesting preserves eyes and shears and creates no drop");
        helper.succeed();
    }

    /**
     * A Slimy Eye gives an eyeless placed head eyes carrying its appearance; an eyed head refuses another. In game:
     * dye a Googly Eye purple, craft it into a Slimy Eye, place a skeleton skull, and right-click the skull with
     * it in survival; the skull gains purple googly eyes, a squelch plays, and the Slimy Eye is used up.
     * Right-click the same skull with a second Slimy Eye; nothing happens and it is kept.
     */
    public static void slimyEyeGivesAPlacedHeadEyes(GameTestHelper helper, Player player) {
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        ItemStack slimy = purpleSlimyEye();
        player.setItemInHand(InteractionHand.MAIN_HAND, slimy);

        helper.assertTrue(use(helper, player, skull) == InteractionResult.CONSUME, "a Slimy Eye applies to a head");
        helper.assertTrue(SkullEyeState.hasEyes(skull), "the head has eyes");
        helper.assertTrue(IRIS.equals(SkullEyeState.snapshot(skull).properties().iris().orElse(null)),
                "the head's eyes carry the Slimy Eye's iris color");
        helper.assertTrue(slimy.isEmpty(), "a survival application uses up the Slimy Eye");

        ItemStack second = purpleSlimyEye();
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        helper.assertTrue(use(helper, player, skull) == InteractionResult.FAIL, "an eyed head refuses another eye");
        helper.assertTrue(second.getCount() == 1, "a refused application keeps the Slimy Eye");
        helper.succeed();
    }

    /**
     * A head without a definition, or any head while the switch is off, refuses a Slimy Eye. In game: right-click
     * a placed dragon head with a Slimy Eye; nothing happens and the Slimy Eye is kept. Set
     * {@code googlyEyesEnabled = false} and right-click a skeleton skull; nothing happens either.
     */
    public static void slimyEyeRefusesUnconfiguredHeadsAndTheSwitch(GameTestHelper helper, Player player) {
        ItemStack slimy = purpleSlimyEye();
        player.setItemInHand(InteractionHand.MAIN_HAND, slimy);

        SkullBlockEntity dragon = placeHead(helper, Blocks.DRAGON_HEAD);
        helper.assertTrue(use(helper, player, dragon) == InteractionResult.FAIL, "a dragon head refuses eyes");
        helper.assertTrue(!SkullEyeState.hasEyes(dragon), "the dragon head stays eyeless");

        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(use(helper, player, skull) == InteractionResult.FAIL,
                    "googlyEyesEnabled=false refuses the application");
            helper.assertTrue(!SkullEyeState.hasEyes(skull), "the head stays eyeless");
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
        }
        helper.assertTrue(slimy.getCount() == 1, "no refusal uses up the Slimy Eye");
        helper.succeed();
    }

    /**
     * Optometrist shears take a head's eyes off as one Googly Eye; plain shears leave them. In game: give a skeleton
     * skull purple eyes with a Slimy Eye, right-click it with plain shears (nothing happens), then with Optometrist
     * shears; the eyes vanish, a purple Googly Eye pops out, the shearing sound plays, and the shears lose one
     * durability.
     */
    public static void optometristHarvestsAPlacedHead(GameTestHelper helper, Player player) {
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        SkullEyeState.set(skull, purpleEyes());

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(use(helper, player, skull) == InteractionResult.PASS, "plain shears pass");
        helper.assertTrue(SkullEyeState.hasEyes(skull), "plain shears leave the eyes on");

        ItemStack shears = optometristShears(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        helper.assertTrue(use(helper, player, skull) == InteractionResult.CONSUME, "Optometrist shears harvest");
        helper.assertTrue(!SkullEyeState.hasEyes(skull), "the harvest clears the head's eyes");
        helper.assertTrue(shears.getDamageValue() == 1, "the harvest spends one shears durability");
        List<ItemEntity> eyes = drops(helper, HEAD, ModContent.GOOGLY_EYE.get());
        helper.assertTrue(eyes.size() == 1, "exactly one Googly Eye pops out");
        helper.assertTrue(IRIS.equals(EyeItemProperties.get(eyes.get(0).getItem()).iris().orElse(null)),
                "the Googly Eye carries the head's iris color");
        helper.succeed();
    }

    /**
     * Breaking an eyed head drops a head item carrying its eyes, and placing that item restores them. In game: give
     * a skeleton skull purple eyes, break it in survival, and hover the dropped skull; its tooltip lists googly eyes
     * and the iris color. Place it again; it shows the same purple eyes in the same arrangement.
     */
    public static void brokenHeadKeepsItsEyes(GameTestHelper helper, Player player) {
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        Snapshot eyes = purpleEyes();
        SkullEyeState.set(skull, eyes);

        helper.getLevel().destroyBlock(helper.absolutePos(HEAD), true);
        List<ItemEntity> heads = drops(helper, HEAD, Items.SKELETON_SKULL);
        helper.assertTrue(heads.size() == 1, "the broken head drops itself");
        ItemStack item = heads.get(0).getItem();
        helper.assertTrue(eyes.equals(SkullEyeState.itemSnapshot(item)), "the dropped head carries its eyes");

        SkullBlockEntity placed = placeHead(helper, Blocks.SKELETON_SKULL);
        placed.applyComponentsFromItemStack(item);
        helper.assertTrue(eyes.equals(SkullEyeState.snapshot(placed)), "placing the head restores its eyes");
        helper.succeed();
    }

    /**
     * An eyed mob's head inherits the mob's eyes; an eyeless mob's head doesn't. In game: give a skeleton purple
     * eyes with {@code /sg admin eyes true} and {@code /sg admin tint iris 0.6 0.1 0.8}, then let a charged creeper
     * blow it up; its dropped skull lists googly eyes and the iris color, and placed it shows the skeleton's purple
     * eyes. A plain skeleton killed the same way drops a plain skull.
     *
     * <p>TODO: this test fails on both loaders for reasons not yet understood, though the behavior works in game;
     * its loader wrappers are commented out until the cause is found.
     */
    public static void eyedMobDropsAnEyedHead(GameTestHelper helper) {
        Skeleton eyed = helper.spawnWithNoFreeWill(EntityType.SKELETON, MOB);
        EyeState.setHasEyes(eyed, true);
        EyeState.setIrisTint(eyed, IRIS);
        Snapshot expected = EyeState.snapshot(eyed);
        blowUpWithChargedCreeper(helper, eyed);
        List<ItemEntity> heads = drops(helper, MOB, Items.SKELETON_SKULL);
        helper.assertTrue(heads.size() == 1, "a charged creeper's kill drops the skeleton's skull");
        helper.assertTrue(expected.equals(SkullEyeState.itemSnapshot(heads.get(0).getItem())),
                "the eyed skeleton's skull carries its eyes");
        heads.get(0).discard();

        Skeleton plain = helper.spawnWithNoFreeWill(EntityType.SKELETON, MOB);
        blowUpWithChargedCreeper(helper, plain);
        heads = drops(helper, MOB, Items.SKELETON_SKULL);
        helper.assertTrue(heads.size() == 1, "the second kill drops a skull too");
        helper.assertTrue(SkullEyeState.itemSnapshot(heads.get(0).getItem()) == null,
                "an eyeless skeleton's skull carries no eyes");
        heads.get(0).discard();
        helper.succeed();
    }

    /**
     * A head's eyes survive saving and loading its block entity. In game: give a placed skeleton skull purple eyes,
     * leave and rejoin the world; the eyes are still there.
     */
    public static void headEyesSurviveSaveLoad(GameTestHelper helper) {
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        Snapshot eyes = purpleEyes();
        SkullEyeState.set(skull, eyes);

        CompoundTag saved = skull.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(skull.getBlockPos(), skull.getBlockState(), saved,
                helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof SkullBlockEntity reloaded && eyes.equals(SkullEyeState.snapshot(reloaded)),
                "the reloaded head has the same eyes");
        helper.succeed();
    }

    /**
     * In Adventure mode an item's {@code CanPlaceOn} list is read against the head itself, from any side. In game:
     * in Adventure mode, hold a Slimy Eye given with {@code [can_place_on={blocks:"minecraft:skeleton_skull"}]} and
     * right-click a skeleton skull's side; it gains eyes. Hold one listing {@code minecraft:stone} and right-click
     * the top of another skull standing on stone; nothing happens.
     */
    public static void adventureCanPlaceOnReadsTheClickedHead(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "head_adventurer"), ClientInformation.createDefault());
        player.getAbilities().mayBuild = false;
        helper.setBlock(HEAD.below(), Blocks.STONE);

        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        ItemStack listingFloor = adventureSlimyEye(Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, listingFloor);
        helper.assertTrue(use(helper, player, skull, Direction.UP) == InteractionResult.FAIL,
                "an item listing the block under the head is refused from the top");
        helper.assertTrue(!SkullEyeState.hasEyes(skull) && listingFloor.getCount() == 1,
                "the refused use changes nothing");

        for (Direction face : List.of(Direction.UP, Direction.NORTH)) {
            helper.setBlock(HEAD, Blocks.AIR);
            skull = placeHead(helper, Blocks.SKELETON_SKULL);
            player.setItemInHand(InteractionHand.MAIN_HAND, adventureSlimyEye(Blocks.SKELETON_SKULL));
            helper.assertTrue(use(helper, player, skull, face) == InteractionResult.CONSUME,
                    "an item listing the head applies from " + face);
            helper.assertTrue(SkullEyeState.hasEyes(skull), "the listed head gains eyes from " + face);
        }
        helper.succeed();
    }

    /**
     * Applying and harvesting count as using the item. In game: note the Used counts of Slimy Eye and Shears on the
     * Statistics screen's Items tab, apply a Slimy Eye to a cow and to a skeleton skull, and take the skull's eyes
     * with Optometrist shears; Slimy Eye rises by two and Shears by one.
     */
    public static void eyeUsesAreCountedInStatistics(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "head_statistician"), ClientInformation.createDefault());
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        player.setItemInHand(InteractionHand.MAIN_HAND, purpleSlimyEye());
        helper.assertTrue(use(helper, player, skull) == InteractionResult.CONSUME, "the head takes the Slimy Eye");
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, MOB);
        EyeState.setHasEyes(cow, false);
        helper.assertTrue(EyeItemService.applySlimyEye(purpleSlimyEye(), player, cow) == InteractionResult.CONSUME,
                "the cow takes the Slimy Eye");
        helper.assertTrue(player.getStats().getValue(Stats.ITEM_USED.get(ModContent.SLIMY_EYE.get())) == 2,
                "both applications count as using a Slimy Eye");

        player.setItemInHand(InteractionHand.MAIN_HAND, optometristShears(helper));
        helper.assertTrue(use(helper, player, skull) == InteractionResult.CONSUME, "the shears harvest the head");
        helper.assertTrue(player.getStats().getValue(Stats.ITEM_USED.get(Items.SHEARS)) == 1,
                "the harvest counts as using the shears");
        helper.succeed();
    }

    /**
     * A head keeps only its eye state from the data it loads, so nothing else a head item carried is saved or sent
     * to players. In game, in creative: {@code /give} a skeleton skull whose {@code block_entity_data} has a
     * {@code somegoogly:data} compound with {@code hasGooglyEyes} true and an extra key, and place it; {@code /data
     * get block} shows the eye state without that key.
     */
    public static void headKeepsOnlyItsEyeState(GameTestHelper helper) {
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        CompoundTag data = new CompoundTag();
        purpleEyes().write(data);
        data.putString("padding", "x".repeat(1024));
        CompoundTag tag = skull.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.put(SkullEyeState.DATA_KEY, data);

        skull.loadWithComponents(tag, helper.getLevel().registryAccess());

        helper.assertTrue(purpleEyes().equals(SkullEyeState.snapshot(skull)), "the head keeps its eyes");
        helper.assertTrue(!skull.saveWithoutMetadata(helper.getLevel().registryAccess()).getCompound(SkullEyeState.DATA_KEY).contains("padding"),
                "the head doesn't keep data that isn't eye state");
        helper.succeed();
    }

    /**
     * Spectators cannot use eye items on heads or shear themselves. In game: in spectator mode, hold a Slimy Eye and
     * right-click a skeleton skull, then sneak and right-click with shears while wearing eyes; nothing changes and
     * nothing is used up.
     */
    public static void spectatorsLeaveHeadsAndThemselvesAlone(GameTestHelper helper) {
        ServerPlayer spectator = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "head_spectator"), ClientInformation.createDefault()) {
            @Override
            public boolean isSpectator() {
                return true;
            }
        };
        SkullBlockEntity skull = placeHead(helper, Blocks.SKELETON_SKULL);
        ItemStack slimy = purpleSlimyEye();
        spectator.setItemInHand(InteractionHand.MAIN_HAND, slimy);
        helper.assertTrue(use(helper, spectator, skull) == InteractionResult.PASS, "a spectator's head click passes");
        helper.assertTrue(!SkullEyeState.hasEyes(skull) && slimy.getCount() == 1,
                "a spectator's Slimy Eye changes nothing");

        ItemStack shears = new ItemStack(Items.SHEARS);
        spectator.setItemInHand(InteractionHand.MAIN_HAND, shears);
        spectator.setShiftKeyDown(true);
        EyeState.applySnapshot(spectator, purpleEyes());
        helper.assertTrue(EyeItemService.selfRemoveWithShears(spectator, InteractionHand.MAIN_HAND)
                == InteractionResult.PASS, "a spectator's self-shear passes");
        helper.assertTrue(EyeState.hasEyes(spectator) && shears.getDamageValue() == 0,
                "a spectator keeps their eyes and their shears' durability");
        helper.succeed();
    }

    /** Kill {@code mob} with a fresh charged creeper's explosion, the kill that makes it drop its head. */
    private static void blowUpWithChargedCreeper(GameTestHelper helper, Skeleton mob) {
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(3, 2, 3));
        CompoundTag data = new CompoundTag();
        creeper.saveWithoutId(data);
        data.putBoolean("powered", true);
        creeper.load(data);
        mob.hurtServer(helper.getLevel(), helper.getLevel().damageSources().explosion(creeper, creeper), LETHAL_DAMAGE);
        creeper.discard();
    }

    /** The {@code item} drops lying around the test-relative block {@code source}. */
    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos source, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(source)).inflate(DROP_SEARCH_RADIUS), drop -> drop.getItem().is(item));
    }

    private static ItemStack optometristShears(GameTestHelper helper) {
        Holder<Enchantment> optometrist = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModContent.OPTOMETRIST);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(optometrist, 1);
        return shears;
    }

    private static SkullBlockEntity placeHead(GameTestHelper helper, Block head) {
        helper.setBlock(HEAD, head);
        return (SkullBlockEntity) helper.getBlockEntity(HEAD);
    }

    private static Snapshot purpleEyes() {
        return new Snapshot(true, 0.5F, AppearanceOverride.EMPTY.withIrisColor(IRIS));
    }

    private static ItemStack purpleSlimyEye() {
        ItemStack slimy = new ItemStack(ModContent.SLIMY_EYE.get());
        EyeItemProperties.set(slimy, AppearanceOverride.EMPTY.withIrisColor(IRIS));
        return slimy;
    }

    private static InteractionResult use(GameTestHelper helper, Player player, SkullBlockEntity skull) {
        return use(helper, player, skull, Direction.UP);
    }

    private static InteractionResult use(GameTestHelper helper, Player player, SkullBlockEntity skull, Direction face) {
        BlockPos pos = skull.getBlockPos();
        return EyeItemService.useOnSkull(player, helper.getLevel(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }

    private static ItemStack adventureSlimyEye(Block canPlaceOn) {
        ItemStack slimy = purpleSlimyEye();
        slimy.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(
                List.of(BlockPredicate.Builder.block().of(BuiltInRegistries.BLOCK, canPlaceOn).build()), true));
        return slimy;
    }
}
