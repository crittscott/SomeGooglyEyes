package com.github.crittscott.somegoogly.registry;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.GooglyEyeItemFactory;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.platform.Registration;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.recipe.SlimyEyeRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.function.Supplier;

/**
 * Everything the mod registers, declared once through {@link Registration}: the Googly Eye (an ingredient) and
 * the Slimy Eye it crafts into (the applicator), the appearance component both carry, the eye-state component of
 * a mob head item, the mod's creative tab, and the eye-modifier and Slimy Eye recipe serializers. The Optometrist
 * enchantment and self-shear damage type are data-driven, so only their resource keys live here.
 */
public final class ModContent {

    // The registered fields are in registration order, not alphabetical: Fabric registers each one as it
    // initializes, the data components before the items whose stacks carry them, and the creative tab reads
    // the items.
    public static final Supplier<DataComponentType<AppearanceOverride>> EYE_PROPERTIES = Registration.dataComponent(
            "eye_properties", () -> DataComponentType.<AppearanceOverride>builder()
                    .persistent(AppearanceOverride.CODEC)
                    .networkSynchronized(AppearanceOverride.STREAM_CODEC)
                    .build());

    /** The eye state a mob head item carries, which placing it hands to the head block entity. */
    public static final Supplier<DataComponentType<EyeState.Snapshot>> HEAD_EYES = Registration.dataComponent(
            "head_eyes", () -> DataComponentType.<EyeState.Snapshot>builder()
                    .persistent(EyeState.Snapshot.CODEC)
                    .networkSynchronized(EyeState.Snapshot.STREAM_CODEC)
                    .build());

    public static final Supplier<GooglyEyeItem> GOOGLY_EYE = Registration.item(
            "googly_eye", () -> GooglyEyeItemFactory.create(new Item.Properties()));

    public static final Supplier<SlimyEyeItem> SLIMY_EYE = Registration.item(
            "slimy_eye", () -> new SlimyEyeItem(new Item.Properties()));

    /**
     * The mod's own creative tab, icon'd with the googly eye. It gathers everything the mod adds — the
     * eye, the slimy eye that applies it, and an Optometrist book — in one place rather than scattered
     * across vanilla tabs.
     */
    public static final Supplier<CreativeModeTab> CREATIVE_TAB = Registration.creativeTab(
            "googly",
            Component.translatable("itemGroup." + SomeGooglyCommon.MOD_ID),
            () -> new ItemStack(GOOGLY_EYE.get()),
            (params, output) -> {
                output.accept(GOOGLY_EYE.get());
                output.accept(SLIMY_EYE.get());
                output.accept(optometristBook(params.holders()));
            });

    public static final Supplier<SimpleCraftingRecipeSerializer<EyeModifierRecipe>> EYE_MODIFIER_RECIPE =
            Registration.recipeSerializer(
                    "eye_modifier", () -> new SimpleCraftingRecipeSerializer<>(EyeModifierRecipe::new));

    public static final Supplier<SlimyEyeRecipe.Serializer> SLIMY_EYE_RECIPE =
            Registration.recipeSerializer("slimy_eye", SlimyEyeRecipe.Serializer::new);

    public static final ResourceKey<Enchantment> OPTOMETRIST = ResourceKey.create(
            Registries.ENCHANTMENT,
            SomeGooglyCommon.id("optometrist"));

    /**
     * The data-driven damage type plain shears deal when a player shears off their own eyes. It has no
     * attacker, so PvP, team, and claim rules that guard player-on-player damage do not cancel it.
     */
    public static final ResourceKey<DamageType> SELF_SHEAR = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            SomeGooglyCommon.id("self_shear"));

    /**
     * Items that harvest eyes as shears: vanilla shears plus the conventional {@code c:tools/shear} tag,
     * so modded shears count without extending {@code ShearsItem}.
     */
    public static final TagKey<Item> SHEARS = TagKey.create(
            Registries.ITEM,
            SomeGooglyCommon.id("shears"));

    private ModContent() {
    }

    /** Register everything above; called once from each loader's entry point, before the loader registers. */
    public static void init() {
        // Loading the class runs the registrations in the field initializers.
    }

    /** An enchanted book holding {@link #OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook(HolderLookup.Provider registries) {
        Holder<Enchantment> optometrist = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(OPTOMETRIST);
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(optometrist, 1));
    }
}
