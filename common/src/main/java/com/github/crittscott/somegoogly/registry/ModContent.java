package com.github.crittscott.somegoogly.registry;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.enchant.OptometristEnchantment;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.GooglyEyeItemFactory;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.platform.Registration;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.recipe.SlimyEyeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.function.Supplier;

/**
 * Everything the mod registers, declared once and registered through {@link Registration}:
 * the Googly Eye (an ingredient) and the Slimy Eye it crafts into (the applicator), which carry the same
 * appearance NBT; the shears-only Optometrist enchantment; the mod's creative tab; and the serializers for
 * the eye-modifier and Slimy Eye recipes. The self-shear damage type is data-driven, so only its resource
 * key lives here.
 */
public final class ModContent {

    public static final Supplier<Enchantment> OPTOMETRIST =
            Registration.enchantment("optometrist", OptometristEnchantment::new);

    public static final Supplier<GooglyEyeItem> GOOGLY_EYE =
            Registration.item("googly_eye", GooglyEyeItemFactory::create);
    public static final Supplier<SlimyEyeItem> SLIMY_EYE = Registration.item("slimy_eye", SlimyEyeItem::new);

    /**
     * The mod's own creative tab, icon'd with the googly eye. It gathers everything the mod adds (the
     * eye, the slimy eye that applies it, and an Optometrist book) in one place rather than scattered
     * across vanilla tabs.
     */
    public static final Supplier<CreativeModeTab> CREATIVE_TAB = Registration.creativeTab(
            "googly",
            Component.translatable("itemGroup." + SomeGooglyCommon.MOD_ID),
            () -> new ItemStack(GOOGLY_EYE.get()),
            (params, output) -> {
                output.accept(GOOGLY_EYE.get());
                output.accept(SLIMY_EYE.get());
                output.accept(optometristBook());
            });

    public static final Supplier<SimpleCraftingRecipeSerializer<EyeModifierRecipe>> EYE_MODIFIER_RECIPE =
            Registration.recipeSerializer("eye_modifier",
                    () -> new SimpleCraftingRecipeSerializer<>(EyeModifierRecipe::new));
    public static final Supplier<SlimyEyeRecipe.Serializer> SLIMY_EYE_RECIPE =
            Registration.recipeSerializer("slimy_eye", SlimyEyeRecipe.Serializer::new);

    /**
     * The data-driven damage type plain shears deal when a player shears off their own eyes. It has no
     * attacker, so PvP, team, and claim rules that guard player-on-player damage do not cancel it.
     */
    public static final ResourceKey<DamageType> SELF_SHEAR = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(SomeGooglyCommon.MOD_ID, "self_shear"));

    /**
     * Items that harvest eyes as shears: vanilla shears plus the conventional {@code c:shears} and
     * {@code forge:shears} tags, so modded shears count without extending {@code ShearsItem}.
     */
    public static final TagKey<Item> SHEARS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(SomeGooglyCommon.MOD_ID, "shears"));

    private ModContent() {
    }

    /**
     * Load this class, which registers everything declared above. Called once from each loader's entry point,
     * while the loader still accepts registrations.
     */
    public static void init() {
    }

    /** An enchanted book holding {@link #OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook() {
        Enchantment optometrist = OPTOMETRIST.get();
        return EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(optometrist, optometrist.getMaxLevel()));
    }
}
