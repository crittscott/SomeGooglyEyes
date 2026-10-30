package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

/**
 * Eye state of a placed mob head and of a head item. A head carries the same {@link Snapshot} a mob does,
 * in a compound a Mixin adds to the head block entity ({@link Holder}), saved under {@value #DATA_KEY} in
 * its data and replaced on every load, so the server's save and the clients' update packets carry it. A
 * head item carries it as the {@link ModContent#HEAD_EYES} component, which the block entity takes and gives
 * as an implicit component, as vanilla's head does its profile: placing the item hands it to the new block
 * entity, and the head's loot copies it back onto the dropped item.
 *
 * <p>A head type borrows its mob's adult eye definition and shows only the eyes attached to its model's head
 * part ({@link #headParts}). A head with no mob here (the dragon, other mods' heads) never has eyes.
 *
 * <p>Server mutations end in a block update, which resends the block entity's data to every client in
 * range; clients only read the compound.
 */
public final class SkullEyeState {

    /** The key of the eye-state compound in a head block entity's saved data. */
    public static final String DATA_KEY = "somegoogly:data";
    /** The last segment of an attachment token that names a model's head part. */
    private static final String HEAD_PART = "head";
    private static final Map<SkullBlock.Type, EntityType<?>> MOBS = Map.of(
            SkullBlock.Types.CREEPER, EntityType.CREEPER,
            SkullBlock.Types.PIGLIN, EntityType.PIGLIN,
            SkullBlock.Types.PLAYER, EntityType.PLAYER,
            SkullBlock.Types.SKELETON, EntityType.SKELETON,
            SkullBlock.Types.WITHER_SKELETON, EntityType.WITHER_SKELETON,
            SkullBlock.Types.ZOMBIE, EntityType.ZOMBIE);

    private SkullEyeState() {
    }

    /** The head block entity's side of the eye-state compound, added by a Mixin. */
    public interface Holder {

        /** The compound, or {@code null} if the head has none; read it, don't write it. */
        @Nullable
        CompoundTag somegoogly$find();

        /** The live compound, created when absent; written in place. */
        CompoundTag somegoogly$get();
    }

    /**
     * Whether a head of {@code entity}'s kind can have eyes under that mob's adult {@code config}: it is usable and
     * has a weighted variant with eyes on its head part.
     */
    public static boolean canHaveEyes(@Nullable ResourceLocation entity, @Nullable RuntimeConfig config) {
        if (entity == null || !RuntimeConfig.isUsable(config)) {
            return false;
        }
        for (Variant variant : config.variants) {
            if (variant.weight > 0 && showsOnHead(variant)) {
                return true;
            }
        }
        return false;
    }

    /** The mob whose eye definition the head {@code block} uses, or {@code null} for any other block. */
    @Nullable
    public static ResourceLocation entityFor(Block block) {
        if (block instanceof AbstractSkullBlock skull) {
            EntityType<?> type = MOBS.get(skull.getType());
            return type == null ? null : EntityType.getKey(type);
        }
        return null;
    }

    /** The mob whose eye definition the head item {@code stack} uses, or {@code null} for any other item. */
    @Nullable
    public static ResourceLocation entityFor(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? entityFor(item.getBlock()) : null;
    }

    /** Whether the placed head has eyes; cheap enough to ask every frame. */
    public static boolean hasEyes(SkullBlockEntity skull) {
        CompoundTag data = ((Holder) skull).somegoogly$find();
        return data != null && data.getBoolean(EyeState.HAS_EYES);
    }

    /**
     * The indexes of {@code helper}'s heads that a head shows, in order: those attached to the model's head part
     * (an attachment token whose last segment is {@value #HEAD_PART}).
     */
    public static int[] headParts(HeadInfo helper) {
        int[] parts = new int[helper.getHeadCount()];
        int count = 0;
        for (int head = 0; head < parts.length; head++) {
            if (isHeadPart(helper.getAttachToken(head))) {
                parts[count++] = head;
            }
        }
        return Arrays.copyOf(parts, count);
    }

    /** Whether eyes attached at {@code token} sit on the model's head part, the part a head shows. */
    private static boolean isHeadPart(@Nullable String token) {
        return token != null && token.substring(token.lastIndexOf('/') + 1).equals(HEAD_PART);
    }

    /** The eye state a head item carries, or {@code null} when it carries none or no eyes. */
    @Nullable
    public static Snapshot itemSnapshot(ItemStack stack) {
        Snapshot snapshot = stack.get(ModContent.HEAD_EYES.get());
        return snapshot != null && snapshot.hasEyes() ? snapshot : null;
    }

    /**
     * Replace the placed head's eye state and send it to every client in range through a block update; server
     * side only.
     */
    public static void set(SkullBlockEntity skull, Snapshot snapshot) {
        snapshot.write(((Holder) skull).somegoogly$get());
        skull.setChanged();
        Level level = skull.getLevel();
        if (level != null) {
            BlockState state = skull.getBlockState();
            level.sendBlockUpdated(skull.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Whether {@code variant} has eyes on the model's head part, the ones a head shows. */
    private static boolean showsOnHead(Variant variant) {
        for (HeadConfig head : variant.heads) {
            if (isHeadPart(head.attachPoint)) {
                return true;
            }
        }
        return false;
    }

    /** The placed head's eye state; {@link Snapshot#NONE} when it has never had any. */
    public static Snapshot snapshot(SkullBlockEntity skull) {
        CompoundTag data = ((Holder) skull).somegoogly$find();
        return data == null ? Snapshot.NONE : Snapshot.read(data);
    }

    /** Store {@code snapshot} on the head item {@code stack}, where placing it hands it to the block entity. */
    public static void stampItem(ItemStack stack, Snapshot snapshot) {
        stack.set(ModContent.HEAD_EYES.get(), snapshot);
    }

    /**
     * The index of the variant of a mob's adult {@code config} that a head with {@code variantRoll} shows: the roll
     * chooses among the variants with eyes on the head part, so a variant with eyes only elsewhere never leaves a
     * head blank.
     */
    public static int variantIndex(@Nullable RuntimeConfig config, float variantRoll) {
        return EyeConfigModel.chooseVariantIndex(config, variantRoll, SkullEyeState::showsOnHead);
    }
}
