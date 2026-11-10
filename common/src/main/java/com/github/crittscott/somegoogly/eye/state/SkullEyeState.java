package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Eye state of a placed mob head and of a head item. A head carries the same {@link Snapshot} a mob does,
 * in a compound a Mixin adds to the head block entity ({@link Holder}), saved under {@value #DATA_KEY} in
 * its data and replaced on every load, so the server's save and the clients' update packets carry it. A
 * head item carries it as the {@link ModContent#HEAD_EYES} component, which the block entity takes and gives
 * as an implicit component, as vanilla's head does its profile: placing the item hands it to the new block
 * entity, and the head's loot copies it back onto the dropped item.
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
    /** Twilight Forest's namespace. Ahead of the maps below, which name it. */
    public static final String TWILIGHT_NAMESPACE = "twilightforest";
    /** Each mob whose head departs from {@link HeadShape#DEFAULT}. */
    private static final Map<ResourceLocation, HeadShape> HEAD_SHAPES = Map.ofEntries(
            Map.entry(EntityType.getKey(EntityType.BEE), HeadShape.showing("body")),
            Map.entry(EntityType.getKey(EntityType.CHICKEN), new HeadShape("head1", "head", true, 0F)),
            Map.entry(EntityType.getKey(EntityType.ENDERMAN), new HeadShape(null, null, false, -0.5F / 16F)),
            Map.entry(EntityType.getKey(EntityType.GHAST), HeadShape.showing("root/body")),
            Map.entry(EntityType.getKey(EntityType.GLOW_SQUID), HeadShape.showing("root")),
            Map.entry(EntityType.getKey(EntityType.MAGMA_CUBE), HeadShape.showing("root/cube0")),
            Map.entry(EntityType.getKey(EntityType.SLIME), new HeadShape("root/right_eye", "root", true, 0F)),
            Map.entry(EntityType.getKey(EntityType.SNOW_GOLEM), new HeadShape(null, null, true, 0.06F)),
            Map.entry(EntityType.getKey(EntityType.SQUID), HeadShape.showing("root")),
            Map.entry(ResourceLocation.fromNamespaceAndPath(TWILIGHT_NAMESPACE, "naga"), HeadShape.showing(EyeConfigModel.attachmentIndex(0))),
            Map.entry(ResourceLocation.fromNamespaceAndPath(TWILIGHT_NAMESPACE, "ur_ghast"), HeadShape.showing("root/body")));
    /**
     * The Heads mod's heads by floor-block id path: the mob whose definition each borrows. Its wall blocks
     * differ only by a {@code _wall} segment. Left out are the heads of mobs with no definition (axolotl,
     * endermite, silverfish).
     */
    public static final Map<String, EntityType<?>> HEADS_MOBS = headsMobs();
    /** The Heads mod's namespace. */
    public static final String HEADS_NAMESPACE = "heads";
    /** Hydra eyes use the trophy model because the mob's heads are separate renderers. */
    public static final ResourceLocation HYDRA_TROPHY_LAYOUT = ResourceLocation.fromNamespaceAndPath(TWILIGHT_NAMESPACE, "hydra_trophy");
    private static final Map<SkullBlock.Type, EntityType<?>> MOBS = Map.of(
            SkullBlock.Types.CREEPER, EntityType.CREEPER,
            SkullBlock.Types.PIGLIN, EntityType.PIGLIN,
            SkullBlock.Types.PLAYER, EntityType.PLAYER,
            SkullBlock.Types.SKELETON, EntityType.SKELETON,
            SkullBlock.Types.WITHER_SKELETON, EntityType.WITHER_SKELETON,
            SkullBlock.Types.ZOMBIE, EntityType.ZOMBIE);
    /** The path segment that sets a wall-mounted head's block id apart from its floor block's. */
    private static final String WALL_SEGMENT = "_wall_";
    /** Supplementaries' spider head, drawn by a modded model. */
    public static final ResourceLocation SUPPLEMENTARIES_SPIDER_HEAD =
            ResourceLocation.fromNamespaceAndPath("supplementaries", "spider_head");

    private SkullEyeState() {
    }

    /**
     * How a head of one mob relates to that mob's eye layout.
     *
     * @param eyePart    the attachment token of the part the head reproduces, or {@code null} for parts named
     *                   {@value EyeConfigModel#PART_HEAD}
     * @param cubePart   the part whose cubes stand for the eye part when fitting a modded head, where it shares its
     *                   pivot with a larger one (the chicken's beak, beside its head), or {@code null} for the eye
     *                   part itself
     * @param fitted     whether a modded head carries the eyes through a fitted placement; false where its face uses
     *                   the mob layout's head-local coordinates
     * @param faceOffset the distance a modded head moves its eyes along the model's Z axis, to clear its face
     */
    public record HeadShape(@Nullable String eyePart, @Nullable String cubePart, boolean fitted, float faceOffset) {

        static final HeadShape DEFAULT = new HeadShape(null, null, true, 0F);

        private static HeadShape showing(String eyePart) {
            return new HeadShape(eyePart, null, true, 0F);
        }
    }

    /**
     * Each head block's mob, built on first use, after every block is registered. Never changed afterward, so
     * the server and render threads can both read it.
     */
    private static final class HeadBlocks {

        static final Map<Block, ResourceLocation> ENTITIES = build();

        private static Map<Block, ResourceLocation> build() {
            Map<Block, ResourceLocation> entities = new IdentityHashMap<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation entity = headEntity(block);
                if (entity != null) {
                    entities.put(block, entity);
                }
            }
            return entities;
        }
    }

    /** The head block entity's side of the eye-state compound, added by a Mixin. */
    public interface Holder {

        /** The compound, or {@code null} if the head has none; read it, don't write it. */
        @Nullable
        CompoundTag somegoogly$find();

        /** The live compound, created when absent; written in place. */
        CompoundTag somegoogly$get();

        /** Save the compound into {@code tag} while the head has eyes; a head without them saves nothing. */
        default void somegoogly$saveEyes(CompoundTag tag) {
            CompoundTag data = somegoogly$find();
            if (data != null && data.getBoolean(EyeState.HAS_EYES)) {
                tag.put(DATA_KEY, data.copy());
            }
        }
    }

    /**
     * Whether a head of {@code entity}'s kind can have eyes under that mob's adult {@code config}: it is usable and
     * has a weighted variant with eyes the head shows.
     */
    public static boolean canHaveEyes(ResourceLocation entity, @Nullable RuntimeConfig config) {
        if (!RuntimeConfig.isUsable(config)) {
            return false;
        }
        for (Variant variant : config.variants) {
            if (variant.weight > 0 && showsOnHead(entity, variant)) {
                return true;
            }
        }
        return false;
    }

    /** The placed head's eye-state compound, or {@code null} if it has none; read it, don't write it. */
    @Nullable
    public static CompoundTag data(@Nullable BlockEntity skull) {
        return skull instanceof Holder holder ? holder.somegoogly$find() : null;
    }

    /**
     * The eye-state compound of a head loading from {@code tag}, or {@code null} if it holds none. Only the state
     * keys are kept: a block entity tag can come from a creative client's item, and whatever is kept here is
     * saved and sent to everyone near the head.
     */
    @Nullable
    public static CompoundTag loadedData(CompoundTag tag) {
        if (!tag.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag data = new CompoundTag();
        Snapshot.read(tag.getCompound(DATA_KEY)).write(data);
        return data;
    }

    /** The mob whose eye definition the head {@code block} uses, or {@code null} for any other block. */
    @Nullable
    public static ResourceLocation entityFor(Block block) {
        return HeadBlocks.ENTITIES.get(block);
    }

    /** The mob whose eye definition the head item {@code stack} uses, or {@code null} for any other item. */
    @Nullable
    public static ResourceLocation entityFor(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? entityFor(item.getBlock()) : null;
    }

    /**
     * The mob whose eye definition a supported modded head block {@code blockId} (floor or wall) uses, or {@code null}
     * for any other block id.
     */
    @Nullable
    public static ResourceLocation entityFor(ResourceLocation blockId) {
        if (SUPPLEMENTARIES_SPIDER_HEAD.equals(blockId)) {
            return EntityType.getKey(EntityType.SPIDER);
        }
        if (!blockId.getNamespace().equals(HEADS_NAMESPACE)) {
            return twilightHead(blockId);
        }
        EntityType<?> type = HEADS_MOBS.get(blockId.getPath().replace(WALL_SEGMENT, "_"));
        return type == null ? null : EntityType.getKey(type);
    }

    @Nullable
    private static ResourceLocation twilightHead(ResourceLocation id) {
        if (!id.getNamespace().equals(TWILIGHT_NAMESPACE)) {
            return null;
        }
        String path = id.getPath().replace(WALL_SEGMENT, "_");
        return switch (path) {
            case "skeleton_skull_candle" -> EntityType.getKey(EntityType.SKELETON);
            case "wither_skeleton_skull_candle" -> EntityType.getKey(EntityType.WITHER_SKELETON);
            case "zombie_skull_candle" -> EntityType.getKey(EntityType.ZOMBIE);
            case "creeper_skull_candle" -> EntityType.getKey(EntityType.CREEPER);
            case "player_skull_candle" -> EntityType.getKey(EntityType.PLAYER);
            case "piglin_skull_candle" -> EntityType.getKey(EntityType.PIGLIN);
            case "hydra_trophy" -> HYDRA_TROPHY_LAYOUT;
            case "naga_trophy", "lich_trophy", "minoshroom_trophy", "knight_phantom_trophy",
                    "ur_ghast_trophy", "alpha_yeti_trophy", "snow_queen_trophy", "quest_ram_trophy" ->
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, path.length() - "_trophy".length()));
            default -> null;
        };
    }

    /** Whether the placed head has eyes; cheap enough to ask every frame. */
    public static boolean hasEyes(BlockEntity skull) {
        CompoundTag data = data(skull);
        return data != null && data.getBoolean(EyeState.HAS_EYES);
    }

    @Nullable
    private static ResourceLocation headEntity(Block block) {
        if (block instanceof AbstractSkullBlock skull) {
            EntityType<?> type = MOBS.get(skull.getType());
            return type == null ? entityFor(BuiltInRegistries.BLOCK.getKey(block)) : EntityType.getKey(type);
        }
        return entityFor(BuiltInRegistries.BLOCK.getKey(block));
    }

    /**
     * The loot function that copies a placed head's eye state onto its dropped item, as the
     * {@link ModContent#HEAD_EYES} component the block entity reports. Every loader's head loot uses it.
     */
    public static LootItemFunction.Builder headLootCopy() {
        return CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                .include(ModContent.HEAD_EYES.get());
    }

    /**
     * The indexes of {@code helper}'s heads, from {@code entity}'s definition, that a head of that mob shows, in
     * order: those attached to the part the head reproduces, by default the model's head part (an attachment
     * token whose last segment is {@value EyeConfigModel#PART_HEAD}).
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
        mobs.put("allay_head", EntityType.ALLAY);
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
        mobs.put("squid_head", EntityType.SQUID);
        mobs.put("glow_squid_head", EntityType.GLOW_SQUID);
        mobs.put("slime_head", EntityType.SLIME);
        mobs.put("magma_cube_head", EntityType.MAGMA_CUBE);
        return Map.copyOf(mobs);
    }

    /** Whether eyes attached at {@code token} show on a head of {@code entity}'s kind ({@link #headParts}). */
    private static boolean isShownPart(ResourceLocation entity, String token) {
        String partToken = shape(entity).eyePart();
        return partToken == null ? EyeConfigModel.lastAttachmentSegment(token).equals(EyeConfigModel.PART_HEAD)
                : token.equals(partToken);
    }

    /** Whether a head item's eye state has eyes; safe on any item. */
    public static boolean itemHasEyes(ItemStack stack) {
        Snapshot snapshot = stack.get(ModContent.HEAD_EYES.get());
        return snapshot != null && snapshot.hasEyes();
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
    public static void set(BlockEntity skull, Snapshot snapshot) {
        snapshot.write(((Holder) skull).somegoogly$get());
        skull.setChanged();
        BlockState state = skull.getBlockState();
        skull.getLevel().sendBlockUpdated(skull.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    /** How a head of {@code entity}'s kind relates to that mob's eye layout. */
    public static HeadShape shape(ResourceLocation entity) {
        return HEAD_SHAPES.getOrDefault(entity, HeadShape.DEFAULT);
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
    public static Snapshot snapshot(@Nullable BlockEntity skull) {
        CompoundTag data = data(skull);
        return data == null ? Snapshot.NONE : Snapshot.read(data);
    }

    /** Store {@code snapshot} on the head item {@code stack}, where placing it hands it to the block entity. */
    public static void stampItem(ItemStack stack, Snapshot snapshot) {
        stack.set(ModContent.HEAD_EYES.get(), snapshot);
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
