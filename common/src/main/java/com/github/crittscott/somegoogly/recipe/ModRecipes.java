package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/** Recipe serializer registry: the special {@link EyeModifierRecipe} and the {@link SlimyEyeRecipe}. */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(SomeGooglyCommon.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<SimpleCraftingRecipeSerializer<EyeModifierRecipe>> EYE_MODIFIER =
            RECIPE_SERIALIZERS.register("eye_modifier",
                    () -> new SimpleCraftingRecipeSerializer<>(EyeModifierRecipe::new));

    public static final RegistrySupplier<SlimyEyeRecipe.Serializer> SLIMY_EYE =
            RECIPE_SERIALIZERS.register("slimy_eye", SlimyEyeRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register() {
        RECIPE_SERIALIZERS.register();
    }
}
