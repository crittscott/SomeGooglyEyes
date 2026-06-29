package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.potion.ModPotions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod's own creative tab, icon'd with the {@link ModItems#GOOGLY_EYE googly eye}. It gathers
 * everything the mod adds — the eye item plus both googly-eyes potions (drinkable and splash) — so they
 * live in one place rather than scattered across vanilla tabs. The potions use {@link ModPotions}'
 * canonical purple stacks; the blue forms vanilla auto-emits for our {@code Potion} are still stripped
 * from the vanilla tabs by {@code ModPotions}.
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SomeGoogly.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GOOGLY = TABS.register("googly", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SomeGoogly.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.GOOGLY_EYE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.GOOGLY_EYE.get());
                        output.accept(optometristBook());
                        output.accept(ModPotions.representativeDrink());
                        output.accept(ModPotions.representativeSplash());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    /** An enchanted book holding {@link ModEnchantments#OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook() {
        Enchantment optometrist = ModEnchantments.OPTOMETRIST.get();
        return EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(optometrist, optometrist.getMaxLevel()));
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). */
    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
