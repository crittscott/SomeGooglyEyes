package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/**
 * The mod's two crafting recipes, driven through a headless 2×1 grid (no player/menu UI):
 *
 * <ul>
 *   <li>{@code eye_modifier} — one googly eye plus one recognized modifier transforms the eye's
 *       {@link AppearanceOverride}. Modifier→color mappings are asserted by presence, not exact RGB, so
 *       they don't pin a particular dye palette.</li>
 *   <li>{@code slimy_eye} — eye plus slimeball, a vanilla {@code crafting_transmute} looked up through the
 *       server's recipe manager, which must carry the eye's appearance onto the applicator.</li>
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

        ItemStack eye = GooglyEyeItem.create(appearance, 1);
        Component customName = Component.literal("keep");
        eye.set(DataComponents.CUSTOM_NAME, customName);
        CraftingInput grid = grid(eye, new ItemStack(Items.SLIME_BALL));
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());

        helper.assertTrue(recipe.isPresent(), "an eye and a slimeball should match a crafting recipe");

        ItemStack result = recipe.get().value().assemble(grid, registries);
        helper.assertTrue(result.is(ModContent.SLIMY_EYE.get()), "the result should be a slimy eye");

        AppearanceOverride carried = EyeItemProperties.get(result);
        helper.assertTrue(iris.equals(carried.iris().orElse(null)), "the slimy eye should carry the eye's iris color");
        helper.assertTrue(carried.glow().orElse(false), "the slimy eye should carry the eye's glow");
        helper.assertTrue(customName.equals(result.get(DataComponents.CUSTOM_NAME)),
                "the slimy eye should carry the eye's other components, such as its name");
        helper.assertTrue(EyeItemProperties.slimyEyeIrisColor(result) == 0xFF336699,
                "the slimy eye should render its iris color with full alpha");
        helper.assertTrue(EyeItemProperties.slimyEyeIrisColor(new ItemStack(ModContent.SLIMY_EYE.get())) == 0xFF000000,
                "the slimy eye should render its default black iris with full alpha");
        helper.succeed();
    }
}
