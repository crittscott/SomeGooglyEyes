package com.github.crittscott.somegoogly.potion;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The "googly eyes" potion. Deliberately carries <b>no {@link net.minecraft.world.effect.MobEffect}s</b>:
 * the eye-giving behavior is single-target, applied by {@code EyePotionInteractions} on splash impact,
 * not by vanilla's area-of-effect potion machinery (which would hit every mob in the cloud). With no
 * effects, vanilla's own splash application is an inert no-op, so we still get its throw arc, break
 * particles, sound, and projectile cleanup for free while layering the single-target logic on top.
 *
 * <p>Obtained by brewing an <b>awkward splash potion</b> (awkward potion + gunpowder) with a harvested
 * {@link ModItems#GOOGLY_EYE}; there is no drinkable form. The eye's appearance is copied onto the
 * resulting potion so the splash tints its target's eyes to match.
 */
public final class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, SomeGoogly.MOD_ID);

    public static final RegistryObject<Potion> GOOGLY_EYES =
            POTIONS.register("googly_eyes", () -> new Potion("googly_eyes"));

    /**
     * Fixed purple tint for every googly-eyes splash, written as the vanilla {@code CustomPotionColor}
     * tag. The potion carries no effects, so without this it would render as plain water-blue; the tag
     * also colors the splash burst and (unlike our {@code EyeProperties}) survives potion-fluid
     * round-trips, so the bottle still reads as "the googly potion" after such a trip.
     */
    private static final int POTION_COLOR = 0x8E44AD;

    private ModPotions() {
    }

    /**
     * A googly-eyes splash potion stack, tinted {@link #POTION_COLOR}. The single creation point for
     * the item so the brewing output and the creative-tab entry can't drift; appearance
     * ({@code EyeProperties}) is layered on by the brewing output on top of this.
     */
    private static ItemStack newGooglySplash() {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), GOOGLY_EYES.get());
        stack.getOrCreateTag().putInt("CustomPotionColor", POTION_COLOR);
        return stack;
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). */
    public static void register(IEventBus modBus) {
        POTIONS.register(modBus);
        modBus.addListener(ModPotions::addToCreativeTabs);
    }

    /**
     * Register the brewing recipe. Must run on the main thread (the brewing registry isn't
     * thread-safe), so callers invoke this from {@code FMLCommonSetupEvent#enqueueWork}.
     */
    public static void registerBrewing() {
        BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                // Splash awkward potion specifically: there is no drinkable googly-eyes potion, so the
                // base must already be a splash (awkward + gunpowder). An item-only Ingredient would
                // also wrongly match any potion, so we check the potion type too.
                return input.is(Items.SPLASH_POTION) && PotionUtils.getPotion(input) == Potions.AWKWARD;
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return ingredient.is(ModItems.GOOGLY_EYE.get());
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                if (isInput(input) && isIngredient(ingredient)) {
                    ItemStack out = newGooglySplash();
                    // Carry the eye's appearance (iris/cornea tint, glow) onto the potion so the splash
                    // applies it to its target. Same AppearanceOverride schema the eye item and mobs share.
                    GooglyEyeItem.setProperties(out, GooglyEyeItem.getProperties(ingredient));
                    return out;
                }
                return ItemStack.EMPTY;
            }
        });
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            // Surface the splash form (the only one with behavior) for testing/creative.
            event.accept(newGooglySplash());
        }
    }
}
