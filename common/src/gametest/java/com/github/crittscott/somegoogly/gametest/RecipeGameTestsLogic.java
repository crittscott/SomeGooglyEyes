package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.recipe.SlimyEyeRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * The mod's two crafting recipes, driven directly through a headless 2×1 grid (no player/menu UI):
 *
 * <ul>
 *   <li>{@code eye_modifier} — one googly eye plus one recognized modifier transforms the eye's
 *       {@link AppearanceOverride}. Modifier→color mappings are asserted by presence, not exact RGB, so
 *       they don't pin a particular dye palette.</li>
 *   <li>{@code slimy_eye} — eye plus slimeball, which must carry the eye's appearance onto the
 *       applicator. That copy is the whole reason the recipe isn't a plain shapeless one.</li>
 * </ul>
 */
public final class RecipeGameTestsLogic {

    private RecipeGameTestsLogic() {
    }

    private static CraftingInput grid(ItemStack eye, ItemStack modifier) {
        return CraftingInput.of(2, 1, List.of(eye, modifier));
    }

    private static EyeModifierRecipe recipe() {
        return new EyeModifierRecipe(CraftingBookCategory.MISC);
    }

    public static void cobwebClearsAllOverrides(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ItemStack tinted = GooglyEyeItem.create(AppearanceOverride.EMPTY.withIrisColor(new EyeColor(1F, 0F, 0F)), 1);
        CraftingInput grid = grid(tinted, new ItemStack(Items.COBWEB));

        helper.assertTrue(recipe().matches(grid, helper.getLevel()), "eye + cobweb should match");
        ItemStack result = recipe().assemble(grid, registries);
        helper.assertTrue(EyeItemProperties.get(result).isEmpty(), "cobweb should clear every override");
        helper.succeed();
    }

    public static void dyeSetsIrisAndKeepsUnrelatedComponent(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ItemStack eye = GooglyEyeItem.create(AppearanceOverride.EMPTY, 1);
        Component customName = Component.literal("keep");
        eye.set(DataComponents.CUSTOM_NAME, customName);
        CraftingInput grid = grid(eye, new ItemStack(Items.RED_DYE));

        helper.assertTrue(recipe().matches(grid, helper.getLevel()), "eye + dye should match");
        ItemStack result = recipe().assemble(grid, registries);
        helper.assertTrue(EyeItemProperties.get(result).iris().isPresent(), "dye should set the iris color");
        helper.assertTrue(customName.equals(result.get(DataComponents.CUSTOM_NAME)),
                "an unrelated stack component should survive the edit");
        helper.succeed();
    }

    public static void glowstoneAndRedstoneToggleGlow(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();

        ItemStack onResult = recipe().assemble(
                grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1), new ItemStack(Items.GLOWSTONE_DUST)), registries);
        helper.assertTrue(EyeItemProperties.get(onResult).glow().orElse(false), "glowstone should force glow on");

        ItemStack offResult = recipe().assemble(
                grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1), new ItemStack(Items.REDSTONE)), registries);
        AppearanceOverride off = EyeItemProperties.get(offResult);
        helper.assertTrue(off.glow().isPresent() && !off.glow().get(), "redstone should force glow off");
        helper.succeed();
    }

    public static void twoEyesDoNotMatch(GameTestHelper helper) {
        CraftingInput grid = grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1),
                GooglyEyeItem.create(AppearanceOverride.EMPTY, 1));
        helper.assertTrue(!recipe().matches(grid, helper.getLevel()), "two eyes and no modifier should not match");
        helper.succeed();
    }

    public static void slimyEyeCarriesTheEyesAppearance(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        AppearanceOverride appearance = AppearanceOverride.EMPTY.withIrisColor(iris).withGlow(true);

        CraftingInput grid = grid(GooglyEyeItem.create(appearance, 1), new ItemStack(Items.SLIME_BALL));
        SlimyEyeRecipe recipe = slimyEyeRecipe();

        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "an eye and a slimeball should match");

        ItemStack result = recipe.assemble(grid, registries);
        helper.assertTrue(result.is(ModItems.SLIMY_EYE.get()), "the result should be a slimy eye");

        AppearanceOverride carried = EyeItemProperties.get(result);
        helper.assertTrue(iris.equals(carried.iris().orElse(null)), "the slimy eye should carry the eye's iris color");
        helper.assertTrue(carried.glow().orElse(false), "the slimy eye should carry the eye's glow");
        helper.succeed();
    }

    private static SlimyEyeRecipe slimyEyeRecipe() {
        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.GOOGLY_EYE.get()), Ingredient.of(Items.SLIME_BALL));
        return new SlimyEyeRecipe("", CraftingBookCategory.MISC,
                new ItemStack(ModItems.SLIMY_EYE.get()), ingredients);
    }
}
