package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/**
 * The mod's two shipped crafting recipes, looked up through the server's recipe manager the way a
 * crafting table does and driven through a headless 2×1 grid (no player/menu UI):
 *
 * <ul>
 *   <li>{@code eye_modifier} — one googly eye plus one recognized modifier transforms the eye's
 *       {@link AppearanceOverride}. Modifier→color mappings are asserted by presence, not exact RGB, so
 *       they don't pin a particular dye palette.</li>
 *   <li>{@code slimy_eye} — eye plus slimeball, a vanilla {@code crafting_transmute}, which must carry
 *       the eye's appearance onto the applicator.</li>
 * </ul>
 */
public final class RecipeGameTestsLogic {

    private static final ResourceLocation EYE_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "eye_modifier");

    private RecipeGameTestsLogic() {
    }

    private static CraftingInput grid(ItemStack eye, ItemStack modifier) {
        return CraftingInput.of(2, 1, List.of(eye, modifier));
    }

    /** The server's crafting recipe for {@code grid}, if any, looked up the way a crafting table does. */
    private static Optional<RecipeHolder<CraftingRecipe>> lookUp(GameTestHelper helper, CraftingInput grid) {
        return helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
    }

    /** Crafts {@code grid}, asserting that the shipped {@code eye_modifier} recipe is the one that matched. */
    private static ItemStack craftWithModifier(GameTestHelper helper, CraftingInput grid) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = lookUp(helper, grid);
        helper.assertTrue(recipe.isPresent() && recipe.get().id().location().equals(EYE_MODIFIER),
                "the grid should match the shipped eye_modifier recipe");
        return recipe.get().value().assemble(grid, helper.getLevel().registryAccess());
    }

    /**
     * A cobweb clears every appearance override. In game: craft a Googly Eye with red dye, then craft the
     * result with a cobweb; the new eye's tooltip lists no colors or glow.
     */
    public static void cobwebClearsAllOverrides(GameTestHelper helper) {
        ItemStack tinted = GooglyEyeItem.create(AppearanceOverride.EMPTY.withIrisColor(new EyeColor(1F, 0F, 0F)), 1);
        ItemStack result = craftWithModifier(helper, grid(tinted, new ItemStack(Items.COBWEB)));
        helper.assertTrue(EyeItemProperties.get(result).isEmpty(), "cobweb should clear every override");
        helper.succeed();
    }

    /**
     * Dye sets the iris color and keeps the eye's other data. In game: rename a Googly Eye in an anvil and craft
     * it with red dye; the result keeps the name and its tooltip shows an iris color.
     */
    public static void dyeSetsIrisAndKeepsUnrelatedComponent(GameTestHelper helper) {
        ItemStack eye = GooglyEyeItem.create(AppearanceOverride.EMPTY, 1);
        Component customName = Component.literal("keep");
        eye.set(DataComponents.CUSTOM_NAME, customName);
        ItemStack result = craftWithModifier(helper, grid(eye, new ItemStack(Items.RED_DYE)));
        helper.assertTrue(EyeItemProperties.get(result).iris().isPresent(), "dye should set the iris color");
        helper.assertTrue(customName.equals(result.get(DataComponents.CUSTOM_NAME)),
                "an unrelated stack component should survive the edit");
        helper.succeed();
    }

    /**
     * Glowstone forces glow on and redstone forces it off. In game: craft a Googly Eye with glowstone dust and
     * its tooltip shows glow on; craft one with redstone dust and it shows glow off.
     */
    public static void glowstoneAndRedstoneToggleGlow(GameTestHelper helper) {
        ItemStack onResult = craftWithModifier(helper,
                grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1), new ItemStack(Items.GLOWSTONE_DUST)));
        helper.assertTrue(EyeItemProperties.get(onResult).glow().orElse(false), "glowstone should force glow on");

        ItemStack offResult = craftWithModifier(helper,
                grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1), new ItemStack(Items.REDSTONE)));
        AppearanceOverride off = EyeItemProperties.get(offResult);
        helper.assertTrue(off.glow().isPresent() && !off.glow().get(), "redstone should force glow off");
        helper.succeed();
    }

    /**
     * Two Googly Eyes with no modifier are not a recipe. In game: put two Googly Eyes in a crafting grid; no
     * result appears.
     */
    public static void twoEyesDoNotMatch(GameTestHelper helper) {
        CraftingInput grid = grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1),
                GooglyEyeItem.create(AppearanceOverride.EMPTY, 1));
        helper.assertTrue(lookUp(helper, grid).isEmpty(), "two eyes and no modifier should match no recipe");
        helper.succeed();
    }

    /**
     * A Slimy Eye keeps the Googly Eye's appearance and name. In game: rename a Googly Eye, craft it with blue
     * dye and glowstone dust, then with a slimeball; the Slimy Eye keeps the name, its tooltip shows the same
     * iris color and glow, and its icon's iris is blue.
     */
    public static void slimyEyeCarriesTheEyesAppearance(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        AppearanceOverride appearance = AppearanceOverride.EMPTY.withIrisColor(iris).withGlow(true);

        ItemStack eye = GooglyEyeItem.create(appearance, 1);
        Component customName = Component.literal("keep");
        eye.set(DataComponents.CUSTOM_NAME, customName);
        CraftingInput grid = grid(eye, new ItemStack(Items.SLIME_BALL));
        Optional<RecipeHolder<CraftingRecipe>> recipe = lookUp(helper, grid);

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
