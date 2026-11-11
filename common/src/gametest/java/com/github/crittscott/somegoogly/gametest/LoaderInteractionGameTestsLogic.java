package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Each loader's own use and player-copy hooks, driven through the loader's event or callback rather than the
 * shared service behind it. Fabric protection-listener ordering remains a manual check.
 */
public final class LoaderInteractionGameTestsLogic {

    private static final BlockPos TARGET = new BlockPos(3, 2, 3);

    private LoaderInteractionGameTestsLogic() {
    }

    /** Fire the loader's player-copy event for {@code original} becoming {@code replacement}. */
    @FunctionalInterface
    public interface Copy {
        void copy(ServerPlayer original, ServerPlayer replacement, boolean alive);
    }

    /**
     * Fire the loader's use event or callback: on {@code hit}'s block when given, else on {@code target} when
     * given, else the held item alone; with {@code refused}, a protection listener refuses it first. Returns the
     * hook's result.
     */
    @FunctionalInterface
    public interface Use {
        InteractionResult interact(ServerPlayer player, @Nullable Entity target, @Nullable BlockHitResult hit,
                                   boolean refused);
    }

    /**
     * Players keep their applied eyes when the game copies them alive (leaving the End) and lose them on death. In
     * game: apply a Slimy Eye tinted to glow to yourself, leave the End through its exit portal, and keep the same
     * eyes; die and respawn, and the applied eyes are gone.
     */
    public static void playerCopiesKeepEyesOnlyWhileAlive(GameTestHelper helper, ServerPlayer template, Copy copy) {
        ServerPlayer original = TestPlayers.create(template, false, 0);
        EyeState.Snapshot state = new EyeState.Snapshot(true, 0.375F, AppearanceOverride.EMPTY.withGlow(true));
        try {
            EyeState.applySnapshot(original, state);
            for (boolean alive : List.of(true, false)) {
                ServerPlayer replacement = TestPlayers.create(template, false, 0);
                try {
                    copy.copy(original, replacement, alive);
                    helper.assertTrue(alive ? EyeState.snapshot(replacement).equals(state)
                                    : !EyeState.hasEyes(replacement) && EyeState.readProperties(replacement).isEmpty(),
                            Component.literal("the loader copy event keeps the whole eye state only while alive"));
                } finally {
                    replacement.discard();
                }
            }
        } finally {
            original.discard();
        }
        helper.succeed();
    }

    private static EyeState.Snapshot snapshot(@Nullable Entity entity, @Nullable SkullBlockEntity block) {
        return block == null ? EyeState.snapshot(entity) : SkullEyeState.snapshot(block);
    }

    /**
     * The loader's use hooks apply and harvest eyes on a cow, a boat, an armor stand, a placed head, and the player,
     * and with {@code testRefusal}, a protection listener's refusal leaves everything as it was. In game: apply and
     * harvest eyes on each with a Slimy Eye and Optometrist shears (sneak and use the shears in the air for
     * yourself); then repeat inside a claim that refuses the click, and the eyes, items, shears durability, your
     * health, and drops stay unchanged. Fabric runs only the registered callbacks here; check its claim ordering by
     * hand.
     */
    public static void useHooksHonorResults(GameTestHelper helper, ServerPlayer template, Use use, boolean testRefusal) {
        ServerPlayer player = TestPlayers.create(template, false, 0);
        player.snapTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0F, 0F);
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            for (String kind : List.of("cow", "boat", "stand", "head", "self")) {
                BlockPos pos = helper.absolutePos(TARGET);
                Entity target = switch (kind) {
                    case "cow" -> helper.spawnWithNoFreeWill(EntityType.COW, TARGET);
                    case "boat" -> helper.spawn(EntityType.OAK_BOAT, TARGET);
                    case "stand" -> helper.spawn(EntityType.ARMOR_STAND, TARGET);
                    default -> null;
                };
                SkullBlockEntity block = null;
                BlockHitResult hit = null;
                if (kind.equals("head")) {
                    helper.setBlock(TARGET, Blocks.SKELETON_SKULL);
                    block = (SkullBlockEntity) helper.getLevel().getBlockEntity(pos);
                    hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                }
                Entity stateTarget = kind.equals("self") ? player : target;
                if (block != null) {
                    SkullEyeState.set(block, EyeState.Snapshot.NONE);
                } else {
                    EyeState.disableAndClearProperties(stateTarget);
                }
                player.setShiftKeyDown(kind.equals("self"));
                for (boolean harvest : List.of(false, true)) {
                    if (kind.equals("self") && !harvest) {
                        // A player can't apply a Slimy Eye to themself; give them eyes to harvest.
                        EyeState.setHasEyes(player, true);
                        continue;
                    }
                    ItemStack stack = new ItemStack(harvest ? Items.SHEARS : ModContent.SLIMY_EYE.get(), 1);
                    if (harvest) {
                        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(ModContent.OPTOMETRIST), 1);
                    }
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    EyeState.Snapshot before = snapshot(stateTarget, block);
                    float health = player.getHealth();
                    List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            player.getBoundingBox().inflate(8));
                    if (testRefusal) {
                        use.interact(player, target, hit, true);
                        helper.assertTrue(before.equals(snapshot(stateTarget, block)),
                                Component.literal("a refused use on the " + kind + " leaves its eyes as they were"));
                        helper.assertTrue(stack.getCount() == 1 && stack.getDamageValue() == 0,
                                Component.literal("a refused use leaves the held item and its durability"));
                        helper.assertTrue(player.getHealth() == health, Component.literal("a refused self-shear does no damage"));
                        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                                player.getBoundingBox().inflate(8)).equals(drops), Component.literal("a refused use drops nothing"));
                    }
                    InteractionResult result = use.interact(player, target, hit, false);
                    helper.assertTrue(result.consumesAction(), Component.literal("the " + kind + " use hook consumes the allowed action"));
                    helper.assertTrue(snapshot(stateTarget, block).hasEyes() != harvest,
                            Component.literal("an allowed use on the " + kind + " changes its eyes"));
                    helper.assertTrue(harvest ? stack.getDamageValue() == 1 : stack.isEmpty(),
                            Component.literal("an allowed use spends exactly its cost"));
                }
                if (target != null) {
                    target.discard();
                }
                if (block != null) {
                    helper.setBlock(TARGET, Blocks.AIR);
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(use.interact(player, null, null, false) == InteractionResult.PASS,
                    Component.literal("an unrelated use passes through the item hook"));
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            player.discard();
        }
        helper.succeed();
    }
}
