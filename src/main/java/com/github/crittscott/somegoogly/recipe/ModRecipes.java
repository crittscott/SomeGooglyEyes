package com.github.crittscott.somegoogly.recipe;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Recipe serializer registry: the special {@link EyeModifierRecipe}. */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SomeGoogly.MOD_ID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<EyeModifierRecipe>> EYE_MODIFIER =
            RECIPE_SERIALIZERS.register("eye_modifier",
                    () -> new SimpleCraftingRecipeSerializer<>(EyeModifierRecipe::new));

    private ModRecipes() {
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). */
    public static void register(IEventBus modBus) {
        RECIPE_SERIALIZERS.register(modBus);
    }
}
