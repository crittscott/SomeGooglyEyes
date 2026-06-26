package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The special {@code eye_modifier} recipe: one googly eye plus one recognized modifier transforms the
 * eye's {@link AppearanceOverride}. Drives the recipe directly through a headless 2×1 crafting grid (no
 * player/menu UI), asserting the appearance delta and that unrelated stack NBT survives. Modifier→color
 * mappings are asserted by presence, not exact RGB, so they don't pin a particular dye palette.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeGameTests {

    private static final String TEMPLATE = "empty";

    private RecipeGameTests() {
    }

    private static CraftingContainer grid(ItemStack eye, ItemStack modifier) {
        AbstractContainerMenu menu = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        CraftingContainer container = new TransientCraftingContainer(menu, 2, 1);
        container.setItem(0, eye);
        container.setItem(1, modifier);
        return container;
    }

    private static EyeModifierRecipe recipe() {
        return new EyeModifierRecipe(new ResourceLocation(SomeGoogly.MOD_ID, "test_eye_modifier"), CraftingBookCategory.MISC);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void cobwebClearsAllOverrides(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ItemStack tinted = GooglyEyeItem.create(AppearanceOverride.EMPTY.withIrisColor(new EyeColor(1F, 0F, 0F)), 1);
        CraftingContainer grid = grid(tinted, new ItemStack(Items.COBWEB));

        helper.assertTrue(recipe().matches(grid, helper.getLevel()), "eye + cobweb should match");
        ItemStack result = recipe().assemble(grid, registries);
        helper.assertTrue(GooglyEyeItem.getProperties(result).isEmpty(), "cobweb should clear every override");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void dyeSetsIrisAndKeepsUnrelatedNbt(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ItemStack eye = GooglyEyeItem.create(AppearanceOverride.EMPTY, 1);
        eye.getOrCreateTag().putString("custom", "keep");
        CraftingContainer grid = grid(eye, new ItemStack(Items.RED_DYE));

        helper.assertTrue(recipe().matches(grid, helper.getLevel()), "eye + dye should match");
        ItemStack result = recipe().assemble(grid, registries);
        helper.assertTrue(GooglyEyeItem.getProperties(result).iris().isPresent(), "dye should set the iris color");
        helper.assertTrue(result.getTag() != null && "keep".equals(result.getTag().getString("custom")),
                "unrelated stack NBT should survive the edit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void glowstoneAndRedstoneToggleGlow(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();

        ItemStack onResult = recipe().assemble(
                grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1), new ItemStack(Items.GLOWSTONE_DUST)), registries);
        helper.assertTrue(GooglyEyeItem.getProperties(onResult).glow().orElse(false), "glowstone should force glow on");

        ItemStack offResult = recipe().assemble(
                grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1), new ItemStack(Items.REDSTONE)), registries);
        AppearanceOverride off = GooglyEyeItem.getProperties(offResult);
        helper.assertTrue(off.glow().isPresent() && !off.glow().get(), "redstone should force glow off");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void twoEyesDoNotMatch(GameTestHelper helper) {
        CraftingContainer grid = grid(GooglyEyeItem.create(AppearanceOverride.EMPTY, 1),
                GooglyEyeItem.create(AppearanceOverride.EMPTY, 1));
        helper.assertTrue(!recipe().matches(grid, helper.getLevel()), "two eyes and no modifier should not match");
        helper.succeed();
    }
}
