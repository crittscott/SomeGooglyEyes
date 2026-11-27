package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.registry.ModContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Googly eye + slimeball → slimy eye, carrying every component of the eye (its appearance, name,
 * and the rest) onto the result, as a vanilla transmute recipe does, so the applied mob ends up
 * with the eye you actually dyed or harvested.
 *
 * <p>A {@link ShapelessRecipe} subclass rather than a {@code CustomRecipe} (as {@link
 * EyeModifierRecipe} is): the ingredients are fixed and declared, so the recipe book and JEI can
 * show it without any plugin of ours. Only the result's components are dynamic, which is what
 * {@link #assemble} copies.
 */
public class SlimyEyeRecipe extends ShapelessRecipe {

    public SlimyEyeRecipe(
            String group,
            CraftingBookCategory category,
            ItemStack result,
            NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModContent.GOOGLY_EYE.get())) {
                return stack.transmuteCopy(result.getItem(), result.getCount());
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<SlimyEyeRecipe> getSerializer() {
        return ModContent.SLIMY_EYE_RECIPE.get();
    }

    /**
     * Parses the ordinary shapeless recipe shape. Vanilla's serializer does all the work and we
     * rebuild its output as our subclass, which it encodes unchanged, so the JSON and the network
     * form stay exactly a shapeless recipe.
     */
    public static class Serializer implements RecipeSerializer<SlimyEyeRecipe> {

        private static final MapCodec<SlimyEyeRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE
                        .codec()
                        .xmap(Serializer::rebuild, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, SlimyEyeRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE
                        .streamCodec()
                        .map(Serializer::rebuild, recipe -> recipe);

        @Override
        public MapCodec<SlimyEyeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SlimyEyeRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        /**
         * {@code recipe} as this subclass; a shapeless recipe's result does not depend on the
         * registries.
         */
        private static SlimyEyeRecipe rebuild(ShapelessRecipe recipe) {
            return new SlimyEyeRecipe(
                    recipe.getGroup(),
                    recipe.category(),
                    recipe.getResultItem(RegistryAccess.EMPTY),
                    recipe.getIngredients());
        }
    }
}
