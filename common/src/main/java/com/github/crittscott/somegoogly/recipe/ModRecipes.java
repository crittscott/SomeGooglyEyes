package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.registry.ContentRegistrar;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/** Recipe serializer registry: the special {@link EyeModifierRecipe} and the {@link SlimyEyeRecipe}. */
public final class ModRecipes {

    public static final ContentRegistrar.Handle<SimpleCraftingRecipeSerializer<EyeModifierRecipe>> EYE_MODIFIER =
            new ContentRegistrar.Handle<>();
    public static final ContentRegistrar.Handle<SlimyEyeRecipe.Serializer> SLIMY_EYE =
            new ContentRegistrar.Handle<>();

    private ModRecipes() {
    }

    public static void register(ContentRegistrar registrar) {
        EYE_MODIFIER.bind(registrar.registerRecipeSerializer(
                "eye_modifier", () -> new SimpleCraftingRecipeSerializer<>(EyeModifierRecipe::new)));
        SLIMY_EYE.bind(registrar.registerRecipeSerializer(
                "slimy_eye", SlimyEyeRecipe.Serializer::new));
    }
}
