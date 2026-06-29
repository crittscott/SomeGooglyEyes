package com.github.crittscott.somegoogly.potion;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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
 * <p>Brewed from an <b>awkward potion</b> and a harvested {@link ModItems#GOOGLY_EYE}, in two forms: a
 * <b>splash</b> (awkward splash + eye) that gives one nearby mob eyes on impact, and a <b>drinkable</b>
 * (awkward potion + eye) that gives the drinker their own eyes — the form that lets a player eye
 * themselves now that players can have eyes. The eye's appearance is copied onto the potion so it tints
 * the recipient's eyes to match. There is no lingering form: an area cloud conflicts with the
 * single-target design, so it is suppressed rather than offered.
 */
public final class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, SomeGoogly.MOD_ID);

    public static final RegistryObject<Potion> GOOGLY_EYES =
            POTIONS.register("googly_eyes", () -> new Potion("googly_eyes"));

    /**
     * Fixed purple tint for every googly-eyes potion (drinkable and splash), written as the vanilla
     * {@code CustomPotionColor} tag. The potion carries no effects, so without this it would render as
     * plain water-blue; the tag also colors the splash burst and (unlike our {@code EyeProperties})
     * survives potion-fluid round-trips, so the bottle still reads as "the googly potion" after one.
     */
    private static final int POTION_COLOR = 0x8E44AD;

    private ModPotions() {
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();

        // Registering a Potion makes vanilla auto-emit an effectless (water-blue) drinkable/splash/
        // lingering for it in Food & Drinks, plus a tipped arrow in Combat. Strip every googly-eyes stack
        // out of those tabs; the real, purple-tinted forms live in the mod's own tab (ModCreativeTabs).
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS || tab == CreativeModeTabs.COMBAT) {
            var it = event.getEntries().iterator();
            while (it.hasNext()) {
                if (PotionUtils.getPotion(it.next().getKey()) == GOOGLY_EYES.get()) {
                    it.remove();
                }
            }
        }
    }

    /**
     * A googly-eyes potion stack of the given bottle ({@link Items#POTION} or {@link Items#SPLASH_POTION}),
     * tinted {@link #POTION_COLOR}. The single creation point for these stacks so the brewing outputs and
     * the creative-tab entries can't drift; appearance ({@code EyeProperties}) is layered on by the
     * brewing output on top of this.
     */
    private static ItemStack newGooglyPotion(Item bottle) {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(bottle), GOOGLY_EYES.get());
        stack.getOrCreateTag().putInt("CustomPotionColor", POTION_COLOR);
        return stack;
    }

    /** The drinkable googly-eyes potion: drinking it gives the drinker their own eyes. */
    private static ItemStack newGooglyDrink() {
        return newGooglyPotion(Items.POTION);
    }

    /** The splash googly-eyes potion: on impact one nearby eligible mob gains eyes. */
    private static ItemStack newGooglySplash() {
        return newGooglyPotion(Items.SPLASH_POTION);
    }

    /**
     * The canonical googly-eyes splash stack with no appearance override — the representative brewing
     * output for displays like JEI ({@code SomeGooglyJeiPlugin}). Routes through {@link #newGooglySplash()}
     * so the shown output can't drift from the real brew or the creative-tab entry.
     */
    public static ItemStack representativeSplash() {
        return newGooglySplash();
    }

    /**
     * The canonical drinkable googly-eyes stack with no appearance override — the representative JEI
     * output for the drinkable brew. Routes through {@link #newGooglyDrink()} so the shown output can't
     * drift from the real brew or the creative-tab entry.
     */
    public static ItemStack representativeDrink() {
        return newGooglyDrink();
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). */
    public static void register(IEventBus modBus) {
        POTIONS.register(modBus);
        modBus.addListener(ModPotions::addToCreativeTabs);
    }

    /**
     * Register the brewing recipes. Must run on the main thread (the brewing registry isn't
     * thread-safe), so callers invoke this from {@code FMLCommonSetupEvent#enqueueWork}.
     *
     * <p>Two parallel recipes, one per bottle: an awkward <b>potion</b> + eye yields the drinkable
     * googly potion, an awkward <b>splash</b> + eye yields the splash. The output keeps the same bottle
     * as the input.
     */
    public static void registerBrewing() {
        BrewingRecipeRegistry.addRecipe(googlyBrew(Items.POTION));
        BrewingRecipeRegistry.addRecipe(googlyBrew(Items.SPLASH_POTION));
    }

    /**
     * A brewing recipe turning an awkward potion of the given {@code bottle} plus a googly eye into the
     * matching googly-eyes potion, copying the eye's appearance onto the output.
     */
    private static IBrewingRecipe googlyBrew(Item bottle) {
        return new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                // The awkward potion in this exact bottle. An item-only Ingredient would wrongly match
                // any potion of that bottle, so we check the potion type too.
                return input.is(bottle) && PotionUtils.getPotion(input) == Potions.AWKWARD;
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return ingredient.is(ModItems.GOOGLY_EYE.get());
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                if (isInput(input) && isIngredient(ingredient)) {
                    ItemStack out = newGooglyPotion(bottle);
                    // Carry the eye's appearance (iris/cornea tint, glow) onto the potion so it applies
                    // to the recipient. Same AppearanceOverride schema the eye item and mobs share.
                    GooglyEyeItem.setProperties(out, GooglyEyeItem.getProperties(ingredient));
                    return out;
                }
                return ItemStack.EMPTY;
            }
        };
    }
}
