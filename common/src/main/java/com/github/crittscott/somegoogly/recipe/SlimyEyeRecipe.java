package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Googly eye + slimeball → slimy eye, carrying the eye's appearance onto the result so the applied
 * mob ends up with the eye you actually dyed or harvested.
 *
 * <p>A wrapped {@link ShapelessRecipe} rather than a {@code CustomRecipe} (as {@link EyeModifierRecipe}
 * is): the ingredients are fixed and declared, so matching, recipe-book placement, and display all come
 * from the vanilla shapeless recipe. Only the result's appearance component is dynamic, which is what
 * {@link #assemble} adds.
 */
public class SlimyEyeRecipe implements CraftingRecipe {

    private final ShapelessRecipe delegate;

    public SlimyEyeRecipe(String group, CraftingBookCategory category, ItemStack result,
                          List<Ingredient> ingredients) {
        this(new ShapelessRecipe(group, category, result, ingredients));
    }

    private SlimyEyeRecipe(ShapelessRecipe delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = delegate.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModContent.GOOGLY_EYE.get())) {
                EyeItemProperties.set(result, EyeItemProperties.get(stack));
                break;
            }
        }
        return result;
    }

    @Override
    public String group() {
        return delegate.group();
    }

    @Override
    public CraftingBookCategory category() {
        return delegate.category();
    }

    @Override
    public PlacementInfo placementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return delegate.display();
    }

    @Override
    public RecipeSerializer<SlimyEyeRecipe> getSerializer() {
        return ModContent.SLIMY_EYE_RECIPE.get();
    }

    /**
     * Parses the ordinary shapeless recipe shape. Vanilla's serializer does all the work and we wrap
     * its output, so the JSON and the network form stay exactly a shapeless recipe.
     */
    public static class Serializer implements RecipeSerializer<SlimyEyeRecipe> {

        private static final MapCodec<SlimyEyeRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(SlimyEyeRecipe::new, recipe -> recipe.delegate);
        private static final StreamCodec<RegistryFriendlyByteBuf, SlimyEyeRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(SlimyEyeRecipe::new, recipe -> recipe.delegate);

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
