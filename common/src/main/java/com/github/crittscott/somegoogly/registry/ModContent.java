package com.github.crittscott.somegoogly.registry;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.GooglyEyeItemFactory;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.recipe.SlimyEyeRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * Everything the mod registers, declared once and bound through each loader's {@link ContentRegistrar}:
 * the Googly Eye (an ingredient) and the Slimy Eye it crafts into (the applicator), the appearance
 * component both carry, the eye-state component of a mob head item, the mod's creative tab, and the
 * eye-modifier and Slimy Eye recipe serializers. The
 * Optometrist enchantment and self-shear damage type are data-driven, so only their resource keys live
 * here.
 */
public final class ModContent {

    public static final ContentRegistrar.Handle<GooglyEyeItem> GOOGLY_EYE = new ContentRegistrar.Handle<>();
    public static final ContentRegistrar.Handle<SlimyEyeItem> SLIMY_EYE = new ContentRegistrar.Handle<>();

    public static final ContentRegistrar.Handle<DataComponentType<AppearanceOverride>> EYE_PROPERTIES =
            new ContentRegistrar.Handle<>();

    /** The eye state a mob head item carries, which placing it hands to the head block entity. */
    public static final ContentRegistrar.Handle<DataComponentType<EyeState.Snapshot>> HEAD_EYES =
            new ContentRegistrar.Handle<>();

    /**
     * The mod's own creative tab, icon'd with the googly eye. It gathers everything the mod adds — the
     * eye, the slimy eye that applies it, and an Optometrist book — in one place rather than scattered
     * across vanilla tabs.
     */
    public static final ContentRegistrar.Handle<CreativeModeTab> CREATIVE_TAB = new ContentRegistrar.Handle<>();

    public static final ContentRegistrar.Handle<SimpleCraftingRecipeSerializer<EyeModifierRecipe>> EYE_MODIFIER_RECIPE =
            new ContentRegistrar.Handle<>();

    public static final ContentRegistrar.Handle<SlimyEyeRecipe.Serializer> SLIMY_EYE_RECIPE =
            new ContentRegistrar.Handle<>();

    public static final ResourceKey<Enchantment> OPTOMETRIST = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "optometrist"));

    /**
     * The data-driven damage type plain shears deal when a player shears off their own eyes. It has no
     * attacker, so PvP, team, and claim rules that guard player-on-player damage do not cancel it.
     */
    public static final ResourceKey<DamageType> SELF_SHEAR = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "self_shear"));

    /**
     * Items that harvest eyes as shears: vanilla shears plus the conventional {@code c:tools/shear} tag,
     * so modded shears count without extending {@code ShearsItem}.
     */
    public static final TagKey<Item> SHEARS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "shears"));

    private ModContent() {
    }

    /** Bind every handle; the data components first, since the items' stacks carry them. */
    public static void register(ContentRegistrar registrar) {
        EYE_PROPERTIES.bind(registrar.registerDataComponent(
                "eye_properties", () -> DataComponentType.<AppearanceOverride>builder()
                        .persistent(AppearanceOverride.CODEC)
                        .networkSynchronized(AppearanceOverride.STREAM_CODEC)
                        .build()));
        HEAD_EYES.bind(registrar.registerDataComponent(
                "head_eyes", () -> DataComponentType.<EyeState.Snapshot>builder()
                        .persistent(EyeState.Snapshot.CODEC)
                        .networkSynchronized(EyeState.Snapshot.STREAM_CODEC)
                        .build()));

        GOOGLY_EYE.bind(registrar.registerItem(
                "googly_eye", () -> GooglyEyeItemFactory.create(new Item.Properties())));
        SLIMY_EYE.bind(registrar.registerItem(
                "slimy_eye", () -> new SlimyEyeItem(new Item.Properties())));

        CREATIVE_TAB.bind(registrar.registerCreativeTab(
                "googly",
                Component.translatable("itemGroup." + SomeGooglyCommon.MOD_ID),
                () -> new ItemStack(GOOGLY_EYE.get()),
                (params, output) -> {
                    output.accept(GOOGLY_EYE.get());
                    output.accept(SLIMY_EYE.get());
                    output.accept(optometristBook(params.holders()));
                }));

        EYE_MODIFIER_RECIPE.bind(registrar.registerRecipeSerializer(
                "eye_modifier", () -> new SimpleCraftingRecipeSerializer<>(EyeModifierRecipe::new)));
        SLIMY_EYE_RECIPE.bind(registrar.registerRecipeSerializer(
                "slimy_eye", SlimyEyeRecipe.Serializer::new));
    }

    /** An enchanted book holding {@link #OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook(HolderLookup.Provider registries) {
        Holder<Enchantment> optometrist = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(OPTOMETRIST);
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(optometrist, 1));
    }
}
