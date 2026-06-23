package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Special crafting recipe that edits a harvested/looted googly eye's appearance: exactly one
 * {@code googly_eye} plus exactly one ingredient recognized by an {@link EyeModifier}. There is no
 * recipe that <i>creates</i> an eye — this only transforms one you already have.
 *
 * <p>The output is a copy of the input eye (preserving any unrelated stack NBT) with the modifier's
 * delta folded onto its {@link AppearanceOverride}.
 */
public class EyeModifierRecipe extends CustomRecipe {

    public EyeModifierRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    /** The eye + the matched modifier ingredient found in a grid, or {@code null} if it doesn't match. */
    private record Match(ItemStack eye, ItemStack modifierStack, EyeModifier modifier) {
    }

    @Nullable
    private static Match find(CraftingContainer container) {
        ItemStack eye = ItemStack.EMPTY;
        ItemStack modifierStack = ItemStack.EMPTY;
        EyeModifier modifier = null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.GOOGLY_EYE.get())) {
                if (!eye.isEmpty()) {
                    return null; // more than one eye
                }
                eye = stack;
                continue;
            }
            EyeModifier match = EyeModifier.find(stack);
            if (match == null) {
                return null; // an ingredient we don't understand
            }
            if (modifier != null) {
                return null; // more than one modifier
            }
            modifierStack = stack;
            modifier = match;
        }

        if (eye.isEmpty() || modifier == null) {
            return null;
        }
        return new Match(eye, modifierStack, modifier);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return find(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        Match match = find(container);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        // Copy (not GooglyEyeItem.create) so any unrelated NBT on the eye survives the edit.
        ItemStack result = match.eye().copyWithCount(1);
        AppearanceOverride updated = match.modifier().apply(GooglyEyeItem.getProperties(result), match.modifierStack());
        GooglyEyeItem.setProperties(result, updated);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        // Representative stack for recipe-book display; the real output is computed in assemble().
        return new ItemStack(ModItems.GOOGLY_EYE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EYE_MODIFIER.get();
    }
}
