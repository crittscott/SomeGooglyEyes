package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.registry.ModContent;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Googly eye + slimeball → slimy eye, carrying all of the eye's NBT (its appearance, name, and the rest)
 * onto the result, as a vanilla transmute does, so the applied mob ends up with the eye you actually dyed
 * or harvested.
 *
 * <p>A {@link ShapelessRecipe} subclass rather than a {@code CustomRecipe} (as {@link EyeModifierRecipe}
 * is): the ingredients are fixed and declared, so the recipe book and JEI can show it without any
 * plugin of ours. Only the result's NBT is dynamic, which is what {@link #assemble} copies.
 */
public class SlimyEyeRecipe extends ShapelessRecipe {

    public SlimyEyeRecipe(ResourceLocation id, String group, CraftingBookCategory category,
                           ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(ModContent.GOOGLY_EYE.get())) {
                CompoundTag tag = stack.getTag();
                result.setTag(tag == null ? null : tag.copy());
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<SlimyEyeRecipe> getSerializer() {
        return ModContent.SLIMY_EYE_RECIPE.get();
    }

    /**
     * Parses the ordinary shapeless recipe shape. Vanilla's serializer does all the work and we rebuild
     * its output as our subclass, so the JSON and the network form stay exactly a shapeless recipe.
     */
    public static class Serializer implements RecipeSerializer<SlimyEyeRecipe> {

        private static final ShapelessRecipe.Serializer SHAPELESS = new ShapelessRecipe.Serializer();

        private static SlimyEyeRecipe rebuild(ShapelessRecipe base) {
            return new SlimyEyeRecipe(base.getId(), base.getGroup(), base.category(),
                    base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
        }

        @Override
        public SlimyEyeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return rebuild(SHAPELESS.fromJson(id, json));
        }

        @Override
        public SlimyEyeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return rebuild(SHAPELESS.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SlimyEyeRecipe recipe) {
            SHAPELESS.toNetwork(buffer, recipe);
        }
    }
}
