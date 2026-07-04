package com.github.crittscott.somegoogly.client.compat.jei;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.potion.ModPotions;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;

/**
 * JEI integration. The googly-eyes brew is a custom {@link net.minecraftforge.common.brewing.IBrewingRecipe}
 * (it copies the eye item's appearance NBT onto the output, which the static {@code BrewingRecipe} class
 * can't do), and JEI's {@code BrewingRecipeMaker} only introspects vanilla/Forge concrete brewing recipes —
 * so without this plugin JEI logs "Can't handle brewing recipe class" and the brew is invisible in the
 * recipe lookup. Here we hand JEI a representative entry for the brewing category so players can discover it.
 *
 * <p>This class is referenced only by JEI's own {@code @JeiPlugin} annotation scan, never by mod code, so
 * the JEI API stays a compile-only soft dependency: with JEI absent the class is simply never loaded.
 *
 * <p>The brew comes in two bottles — a drinkable (awkward potion + eye) and a splash (awkward splash +
 * eye) — so we register one representative entry per bottle.
 */
@JeiPlugin
public class SomeGooglyJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(SomeGoogly.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        ItemStack eye = new ItemStack(ModItems.GOOGLY_EYE.get());

        // Awkward (drinkable) potion + googly eye → drinkable googly-eyes, and awkward splash + googly
        // eye → googly-eyes splash. The displayed eye/output carry no appearance override; the real brew
        // copies whatever the eye item holds onto its output.
        ItemStack awkwardDrink = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD);
        ItemStack awkwardSplash = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.AWKWARD);

        IJeiBrewingRecipe drink = factory.createBrewingRecipe(
                List.of(eye), awkwardDrink, ModPotions.representativeDrink());
        IJeiBrewingRecipe splash = factory.createBrewingRecipe(
                List.of(eye), awkwardSplash, ModPotions.representativeSplash());

        registration.addRecipes(RecipeTypes.BREWING, List.of(drink, splash));
    }
}
