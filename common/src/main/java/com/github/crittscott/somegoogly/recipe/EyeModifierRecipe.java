package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Special crafting recipe that edits a harvested/looted googly eye's appearance: exactly one
 * {@code googly_eye} plus exactly one modifier ingredient (see {@link #modify}). There is no recipe
 * that <i>creates</i> an eye — this only transforms one you already have.
 *
 * <p>The output is a copy of the input eye (preserving unrelated data components) with the
 * modifier's delta folded onto its {@link AppearanceOverride}.
 */
public class EyeModifierRecipe extends CustomRecipe {

    public EyeModifierRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The eye and the modifier ingredient found in a grid. */
    private record Match(ItemStack eye, ItemStack modifierStack) {
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Match match = find(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        // Copy (not a fresh stack) so unrelated components on the eye survive the edit.
        ItemStack result = match.eye().copyWithCount(1);
        EyeItemProperties.set(result, modify(EyeItemProperties.get(result), match.modifierStack()));
        return result;
    }

    /**
     * {@code current} with the modifier ingredient's change layered on, or {@code null} when
     * {@code stack} is not a modifier: a vanilla dye sets that dye's color on the iris (set, not
     * blended), glowstone dust forces glow on, redstone dust forces it off, and a cobweb strips every
     * override, leaving a bare eye that falls back to config appearance. Geometry stays in config.
     */
    @Nullable
    private static AppearanceOverride modify(AppearanceOverride current, ItemStack stack) {
        if (stack.getItem() instanceof DyeItem dye) {
            return current.withIrisColor(EyeColor.fromRgb24(dye.getDyeColor().getTextureDiffuseColor()));
        }
        if (stack.is(Items.GLOWSTONE_DUST)) {
            return current.withGlow(true);
        }
        if (stack.is(Items.REDSTONE)) {
            return current.withGlow(false);
        }
        if (stack.is(Items.COBWEB)) {
            return AppearanceOverride.EMPTY;
        }
        return null;
    }

    @Nullable
    private static Match find(CraftingInput input) {
        ItemStack eye = ItemStack.EMPTY;
        ItemStack modifierStack = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModContent.GOOGLY_EYE.get())) {
                if (!eye.isEmpty()) {
                    return null; // more than one eye
                }
                eye = stack;
                continue;
            }
            if (modify(AppearanceOverride.EMPTY, stack) == null) {
                return null; // an ingredient we don't understand
            }
            if (!modifierStack.isEmpty()) {
                return null; // more than one modifier
            }
            modifierStack = stack;
        }

        if (eye.isEmpty() || modifierStack.isEmpty()) {
            return null;
        }
        return new Match(eye, modifierStack);
    }

    @Override
    public RecipeSerializer<EyeModifierRecipe> getSerializer() {
        return ModContent.EYE_MODIFIER_RECIPE.get();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }
}
