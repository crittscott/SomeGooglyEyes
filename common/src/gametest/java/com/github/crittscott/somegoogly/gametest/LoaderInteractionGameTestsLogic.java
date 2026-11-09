package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native use wiring and copy-event coverage. Fabric protection ordering remains a manual check. */
public final class LoaderInteractionGameTestsLogic {
    private LoaderInteractionGameTestsLogic() {
    }

    @FunctionalInterface
    public interface Copy {
        void copy(ServerPlayer original, ServerPlayer replacement, boolean alive);
    }

    @FunctionalInterface
    public interface Use {
        InteractionResult interact(ServerPlayer player, @Nullable Entity target, @Nullable BlockHitResult hit,
            boolean refused);
    }

    /**
     * In game: apply a tinted Slimy Eye to yourself, leave the End through its exit portal, and keep the same
     * eyes. Die and respawn: applied eyes are gone. These tests exercise each loader's copy event.
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
                        "the loader copy event retains the whole snapshot only while alive");
                } finally {
                    replacement.discard();
                }
            }
        } finally {
            original.discard();
        }
        helper.succeed();
    }

    private static EyeState.Snapshot snapshot(@Nullable Entity entity, @Nullable BlockEntity block) {
        return block == null ? EyeState.snapshot(entity) : SkullEyeState.snapshot(block);
    }

    /**
     * In game: apply and harvest eyes on a cow, boat, armor stand, and head, then self-shear. On Forge repeat
     * inside a claim that refuses the click: state, items, durability, health, and drops stay unchanged.
     * Fabric executes the normal registered callbacks here; repeat its claim-ordering check manually.
     */
    public static void useHooksHonorResults(GameTestHelper helper, ServerPlayer template, Use use, boolean testRefusal) {
        ServerPlayer player = TestPlayers.create(template, false, 0);
        player.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0F, 0F);
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            for (String kind : List.of("cow", "boat", "stand", "head", "self")) {
                BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
                Entity target = switch (kind) {
                    case "cow" -> helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 2, 3));
                    case "boat" -> helper.spawn(EntityType.BOAT, new BlockPos(3, 2, 3));
                    case "stand" -> helper.spawn(EntityType.ARMOR_STAND, new BlockPos(3, 2, 3));
                    default -> null;
                };
                BlockEntity block = null;
                BlockHitResult hit = null;
                if (kind.equals("head")) {
                    helper.setBlock(new BlockPos(3, 2, 3), Blocks.SKELETON_SKULL);
                    block = helper.getLevel().getBlockEntity(pos);
                    hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                }
                Entity stateTarget = kind.equals("self") ? player : target;
                if (block != null) SkullEyeState.set(block, EyeState.Snapshot.NONE);
                else EyeState.disableAndClearProperties(stateTarget);
                player.setShiftKeyDown(kind.equals("self"));
                for (boolean harvest : List.of(false, true)) {
                    if (kind.equals("self") && !harvest) {
                        EyeState.setHasEyes(player, true);
                        continue;
                    }
                    ItemStack stack = new ItemStack(harvest ? Items.SHEARS : ModContent.SLIMY_EYE.get(), 1);
                    if (harvest) stack.enchant(ModContent.OPTOMETRIST.get(), 1);
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    var before = snapshot(stateTarget, block);
                    float health = player.getHealth();
                    var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8));
                    if (testRefusal) {
                        use.interact(player, target, hit, true);
                        helper.assertTrue(before.equals(snapshot(stateTarget, block)), "refused " + kind + " preserves eye state");
                        helper.assertTrue(stack.getCount() == 1 && stack.getDamageValue() == 0, "refusal preserves the held tool/item");
                        helper.assertTrue(player.getHealth() == health, "refused self-shear causes no damage");
                        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            player.getBoundingBox().inflate(8)).equals(drops), "refusal emits no drops");
                    }
                    InteractionResult result = use.interact(player, target, hit, false);
                    helper.assertTrue(result.consumesAction(), "registered " + kind + " hook consumes its allowed action");
                    helper.assertTrue(snapshot(stateTarget, block).hasEyes() != harvest, "allowed use changes the intended target");
                    helper.assertTrue(harvest ? stack.getDamageValue() == 1 : stack.isEmpty(), "allowed use spends exactly its cost");
                }
                if (target != null) target.discard();
                if (block != null) helper.setBlock(new BlockPos(3, 2, 3), Blocks.AIR);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(use.interact(player, null, null, false) == InteractionResult.PASS,
                "an irrelevant use passes through the item callback");
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            player.discard();
        }
        helper.succeed();
    }
}
