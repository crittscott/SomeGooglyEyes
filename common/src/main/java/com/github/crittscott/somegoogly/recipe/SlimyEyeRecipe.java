package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Googly eye + slimeball → slimy eye, carrying the eye's appearance onto the result so the applied
 * mob ends up with the eye you actually dyed or harvested.
 *
 * <p>A {@link ShapelessRecipe} subclass rather than a {@code CustomRecipe} (as {@link EyeModifierRecipe}
 * is): the ingredients are fixed and declared, so the recipe book and JEI can show it without any
 * plugin of ours. Only the result's appearance component is dynamic, which is what
 * {@link #assemble} adds.
 */
public class SlimyEyeRecipe extends ShapelessRecipe {

    private final ShapelessRecipe delegate;

    public SlimyEyeRecipe(String group, CraftingBookCategory category, ItemStack result,
                          NonNullList<Ingredient> ingredients) {
        this(new ShapelessRecipe(group, category, result, ingredients));
    }

    private SlimyEyeRecipe(ShapelessRecipe delegate) {
        super(delegate.getGroup(), delegate.category(), ItemStack.EMPTY, delegate.getIngredients());
        this.delegate = delegate;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = delegate.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModItems.GOOGLY_EYE.get())) {
                EyeItemProperties.set(result, EyeItemProperties.get(stack));
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SLIMY_EYE.get();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return delegate.getResultItem(registries);
    }

    /**
     * Parses the ordinary shapeless recipe shape. Vanilla's serializer does all the work and we rebuild
     * its output as our subclass, so the JSON and the network form stay exactly a shapeless recipe.
     */
    public static class Serializer implements RecipeSerializer<SlimyEyeRecipe> {

        private static final ShapelessRecipe.Serializer SHAPELESS = new ShapelessRecipe.Serializer();
        private static final MapCodec<SlimyEyeRecipe> CODEC =
                SHAPELESS.codec().xmap(SlimyEyeRecipe::new, recipe -> recipe.delegate);
        private static final StreamCodec<RegistryFriendlyByteBuf, SlimyEyeRecipe> STREAM_CODEC =
                SHAPELESS.streamCodec().map(SlimyEyeRecipe::new, recipe -> recipe.delegate);

        @Override
        public MapCodec<SlimyEyeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SlimyEyeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
