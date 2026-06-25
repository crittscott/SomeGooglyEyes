package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One "what does this ingredient do to an eye" rule, used by {@link EyeModifierRecipe}.
 *
 * <p>The generalized seam for crafting-based appearance edits: a modifier recognizes an ingredient
 * stack and folds a delta onto the eye's {@link AppearanceOverride}. Adding a future appearance edit
 * (glow via glowstone, etc.) is a new entry in {@link #MODIFIERS} — not a new recipe class.
 */
public interface EyeModifier {

    /**
     * The registered modifiers, in match order: dye → iris color, glowstone dust → glow on,
     * redstone → glow off, cobweb → strip all overrides back to a bare eye. They match disjoint
     * ingredients, so order is not significant.
     */
    List<EyeModifier> MODIFIERS = List.of(
            new DyeEyeModifier(),
            new GlowEyeModifier(),
            new UnglowEyeModifier(),
            new StripEyeModifier());

    /** 1 eye + 1 of the 16 vanilla dyes → that dye's color on the iris (set, not blended). */
    final class DyeEyeModifier implements EyeModifier {
        @Override
        public AppearanceOverride apply(AppearanceOverride current, ItemStack stack) {
            DyeColor color = ((DyeItem) stack.getItem()).getDyeColor();
            return current.withIrisColor(EyeColor.of(color.getTextureDiffuseColors()));
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.getItem() instanceof DyeItem;
        }
    }

    /** Glowstone dust → make the eye glow (explicit on, overriding config). */
    final class GlowEyeModifier implements EyeModifier {
        @Override
        public AppearanceOverride apply(AppearanceOverride current, ItemStack stack) {
            return current.withGlow(true);
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(Items.GLOWSTONE_DUST);
        }
    }

    /** Cobweb → strip every override, leaving a bare eye that falls back to config appearance. */
    final class StripEyeModifier implements EyeModifier {
        @Override
        public AppearanceOverride apply(AppearanceOverride current, ItemStack stack) {
            return AppearanceOverride.EMPTY;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(Items.COBWEB);
        }
    }

    /** Redstone dust → turn the glow off (explicit off, overriding config). */
    final class UnglowEyeModifier implements EyeModifier {
        @Override
        public AppearanceOverride apply(AppearanceOverride current, ItemStack stack) {
            return current.withGlow(false);
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(Items.REDSTONE);
        }
    }

    /** Return {@code current} with this modifier's change layered on (geometry stays in config). */
    AppearanceOverride apply(AppearanceOverride current, ItemStack stack);

    /** The first modifier that handles {@code stack}, or {@code null} if none does. */
    @Nullable
    static EyeModifier find(ItemStack stack) {
        for (EyeModifier modifier : MODIFIERS) {
            if (modifier.matches(stack)) {
                return modifier;
            }
        }
        return null;
    }

    /** Whether this modifier applies to the given (non-eye) ingredient stack. */
    boolean matches(ItemStack stack);
}
