package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Eye state of a placed mob head and of a head item. A head carries the same {@link Snapshot} a mob does,
 * in a compound each loader's Mixin adds to the head block entity ({@link Holder}), saved under
 * {@value #DATA_KEY} in its data and replaced on every {@code load}. So the server's save, the clients'
 * update packets, and a head item's {@code BlockEntityTag} all carry it: vanilla's placement loads it into
 * the new block entity, and the head's loot copies it back onto the dropped item. Neither loader's own
 * block-entity store serves here, since a plain {@code load} doesn't read either one back.
 *
 * <p>A head type borrows its mob's adult eye definition and shows only the eyes attached to the part of its mob
 * that the head model reproduces ({@link #headParts}). Vanilla head types are matched by type, the Heads mod's
 * by block id ({@link #entityFor(ResourceLocation)}), so that mod is never referenced and its absence changes
 * nothing. A head with no mob here (the dragon, other mods' heads, Heads' heads whose mob's eyes sit on a part
 * the head doesn't reproduce) never has eyes.
 *
 * <p>Server mutations end in a block update, which resends the block entity's data to every client in
 * range; clients only read the compound.
 */
public final class SkullEyeState {

    /** The key of the eye-state compound in a head block entity's saved data. */
    public static final String DATA_KEY = "somegoogly:data";
    /** The last segment of an attachment token that names a model's head part. */
    private static final String HEAD_PART = "head";
    /**
     * The attachment token of the part a head reproduces, for mobs whose eyes sit on a part other than the head;
     * every other mob's head shows the eyes on parts named {@value #HEAD_PART}.
     */
    private static final Map<ResourceLocation, String> HEAD_PART_TOKENS = Map.of(
            EntityType.getKey(EntityType.BEE), "body",
            EntityType.getKey(EntityType.CHICKEN), "head1",
            EntityType.getKey(EntityType.GHAST), "root/body");
    /**
     * The Heads mod's heads by floor-block id path: the mob whose definition each borrows. Its wall blocks
     * differ only by a {@code _wall} segment. Left out are the heads of mobs with no definition (axolotl,
     * endermite, silverfish) and those whose mob's eyes sit on a part the head model doesn't reproduce (squid,
     * glow squid, slime, magma cube).
     */
    public static final Map<String, EntityType<?>> HEADS_MOBS = headsMobs();
    /** The Heads mod's namespace. */
    public static final String HEADS_NAMESPACE = "heads";
    private static final Map<SkullBlock.Type, EntityType<?>> MOBS = Map.of(
            SkullBlock.Types.CREEPER, EntityType.CREEPER,
            SkullBlock.Types.PIGLIN, EntityType.PIGLIN,
            SkullBlock.Types.PLAYER, EntityType.PLAYER,
            SkullBlock.Types.SKELETON, EntityType.SKELETON,
            SkullBlock.Types.WITHER_SKELETON, EntityType.WITHER_SKELETON,
            SkullBlock.Types.ZOMBIE, EntityType.ZOMBIE);

    private SkullEyeState() {
    }



    /** The head block entity's side of the eye-state compound, added by each loader's Mixin. */
    public interface Holder {

        /** The compound, or {@code null} if the head has none; read it, don't write it. */
        @Nullable
        CompoundTag somegoogly$find();

        /** The live compound, created when absent; written in place. */
        CompoundTag somegoogly$get();
    }

    /**
     * Whether a head of {@code entity}'s kind can have eyes under that mob's adult {@code config}: it is usable and
     * has a weighted variant with eyes the head shows.
     */
    public static boolean canHaveEyes(@Nullable ResourceLocation entity, @Nullable RuntimeConfig config) {
        if (entity == null || !RuntimeConfig.isUsable(config)) {
            return false;
        }
        for (Variant variant : config.variants) {
            if (variant.weight > 0 && showsOnHead(entity, variant)) {
                return true;
            }
        }
        return false;
    }

    /** Copy an eyed head block's snapshot onto its generated head items. */
    public static void copyToLoot(LootContext context, Collection<ItemStack> generatedLoot) {
        if (context.getParamOrNull(LootContextParams.BLOCK_ENTITY) instanceof SkullBlockEntity skull) {
            Snapshot state = snapshot(skull);
            if (state.hasEyes()) {
                for (ItemStack stack : generatedLoot) {
                    if (entityFor(stack) != null) {
                        stampItem(stack, state);
                    }
                }
            }
        }
    }

    /** The mob whose eye definition the head {@code block} uses, or {@code null} for any other block. */
    @Nullable
    public static ResourceLocation entityFor(Block block) {
        if (block instanceof AbstractSkullBlock skull) {
            EntityType<?> type = MOBS.get(skull.getType());
            return type == null ? entityFor(BuiltInRegistries.BLOCK.getKey(block)) : EntityType.getKey(type);
        }
        return null;
    }

    /** The mob whose eye definition the head item {@code stack} uses, or {@code null} for any other item. */
    @Nullable
    public static ResourceLocation entityFor(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? entityFor(item.getBlock()) : null;
    }

    /**
     * The mob whose eye definition the Heads mod's head block {@code blockId} (floor or wall) uses, or {@code null}
     * for any other block id.
     */
    @Nullable
    public static ResourceLocation entityFor(ResourceLocation blockId) {
        if (!blockId.getNamespace().equals(HEADS_NAMESPACE)) {
            return null;
        }
        EntityType<?> type = HEADS_MOBS.get(blockId.getPath().replace("_wall_", "_"));
        return type == null ? null : EntityType.getKey(type);
    }

    /** Whether the placed head has eyes; cheap enough to ask every frame. */
    public static boolean hasEyes(SkullBlockEntity skull) {
        CompoundTag data = ((Holder) skull).somegoogly$find();
        return data != null && data.getBoolean(EyeState.HAS_EYES);
    }

    /**
     * The indexes of {@code helper}'s heads, from {@code entity}'s definition, that a head of that mob shows, in
     * order: those attached to the part the head reproduces, by default the model's head part (an attachment
     * token whose last segment is {@value #HEAD_PART}).
     */
    public static int[] headParts(ResourceLocation entity, HeadInfo helper) {
        int[] parts = new int[helper.getHeadCount()];
        int count = 0;
        for (int head = 0; head < parts.length; head++) {
            if (isShownPart(entity, helper.getAttachToken(head))) {
                parts[count++] = head;
            }
        }
        return Arrays.copyOf(parts, count);
    }

    private static Map<String, EntityType<?>> headsMobs() {
        Map<String, EntityType<?>> mobs = new HashMap<>();
        for (String variant : List.of("all_black", "black", "british_shorthair", "calico", "jellie", "persian",
                "ragdoll", "red", "siamese", "tabby", "white")) {
            mobs.put("cat_" + variant + "_head", EntityType.CAT);
        }
        for (String variant : List.of("black", "brown", "chestnut", "creamy", "darkbrown", "gray", "white")) {
            mobs.put("horse_" + variant + "_head", EntityType.HORSE);
        }
        for (DyeColor color : DyeColor.values()) {
            if (color != DyeColor.WHITE) {
                mobs.put(color.getName() + "_sheep_head", EntityType.SHEEP);
            }
        }
        mobs.put("bat_head", EntityType.BAT);
        mobs.put("bee_head", EntityType.BEE);
        mobs.put("blaze_head", EntityType.BLAZE);
        mobs.put("brown_mooshroom_head", EntityType.MOOSHROOM);
        mobs.put("cave_spider_head", EntityType.CAVE_SPIDER);
        mobs.put("chicken_head", EntityType.CHICKEN);
        mobs.put("cod_head", EntityType.COD);
        mobs.put("cow_head", EntityType.COW);
        mobs.put("dolphin_head", EntityType.DOLPHIN);
        mobs.put("donkey_head", EntityType.DONKEY);
        mobs.put("drowned_head", EntityType.DROWNED);
        mobs.put("enderman_head", EntityType.ENDERMAN);
        mobs.put("evoker_head", EntityType.EVOKER);
        mobs.put("fox_head", EntityType.FOX);
        mobs.put("fox_snow_head", EntityType.FOX);
        mobs.put("ghast_head", EntityType.GHAST);
        mobs.put("goat_head", EntityType.GOAT);
        mobs.put("hoglin_head", EntityType.HOGLIN);
        mobs.put("horse_skeleton_head", EntityType.SKELETON_HORSE);
        mobs.put("horse_undead_head", EntityType.ZOMBIE_HORSE);
        mobs.put("husk_head", EntityType.HUSK);
        mobs.put("illusioner_head", EntityType.ILLUSIONER);
        mobs.put("iron_golem_head", EntityType.IRON_GOLEM);
        mobs.put("mule_head", EntityType.MULE);
        mobs.put("ocelot_head", EntityType.OCELOT);
        mobs.put("pig_head", EntityType.PIG);
        mobs.put("piglin_brute_head", EntityType.PIGLIN_BRUTE);
        mobs.put("piglin_head", EntityType.PIGLIN);
        mobs.put("pillager_head", EntityType.PILLAGER);
        mobs.put("red_mooshroom_head", EntityType.MOOSHROOM);
        mobs.put("salmon_head", EntityType.SALMON);
        mobs.put("sheep_head", EntityType.SHEEP);
        mobs.put("sheep_shaven_head", EntityType.SHEEP);
        mobs.put("snow_golem_head", EntityType.SNOW_GOLEM);
        mobs.put("spider_head", EntityType.SPIDER);
        mobs.put("stray_head", EntityType.STRAY);
        mobs.put("turtle_head", EntityType.TURTLE);
        mobs.put("villager_head", EntityType.VILLAGER);
        mobs.put("vindicator_head", EntityType.VINDICATOR);
        mobs.put("wandering_trader_head", EntityType.WANDERING_TRADER);
        mobs.put("witch_head", EntityType.WITCH);
        mobs.put("wolf_head", EntityType.WOLF);
        mobs.put("zoglin_head", EntityType.ZOGLIN);
        mobs.put("zombie_villager_head", EntityType.ZOMBIE_VILLAGER);
        mobs.put("zombified_piglin_skull", EntityType.ZOMBIFIED_PIGLIN);
        return Map.copyOf(mobs);
    }

    /** Whether eyes attached at {@code token} show on a head of {@code entity}'s kind ({@link #headParts}). */
    private static boolean isShownPart(ResourceLocation entity, @Nullable String token) {
        if (token == null) {
            return false;
        }
        String partToken = HEAD_PART_TOKENS.get(entity);
        return partToken == null ? token.substring(token.lastIndexOf('/') + 1).equals(HEAD_PART)
                : token.equals(partToken);
    }

    /** The eye state a head item carries, or {@code null} when it carries none or no eyes. */
    @Nullable
    public static Snapshot itemSnapshot(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(BlockItem.BLOCK_ENTITY_TAG);
        if (tag == null || !tag.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        Snapshot snapshot = Snapshot.read(tag.getCompound(DATA_KEY));
        return snapshot.hasEyes() ? snapshot : null;
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

    /** Whether {@code variant} of {@code entity}'s definition has eyes a head of that mob shows. */
    private static boolean showsOnHead(ResourceLocation entity, Variant variant) {
        for (HeadConfig head : variant.heads) {
            if (isShownPart(entity, head.attachPoint)) {
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
        CompoundTag data = new CompoundTag();
        snapshot.write(data);
        stack.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG).put(DATA_KEY, data);
    }

    /**
     * The index of the variant of {@code entity}'s adult {@code config} that a head with {@code variantRoll} shows:
     * the roll chooses among the variants with eyes on the part the head reproduces, so a mob's variant with eyes
     * only elsewhere (a cow's on its body) never leaves a head blank.
     */
    public static int variantIndex(ResourceLocation entity, @Nullable RuntimeConfig config, float variantRoll) {
        return EyeConfigModel.chooseVariantIndex(config, variantRoll, variant -> showsOnHead(entity, variant));
    }
}
