package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.registry.ContentRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * The mod's own creative tab, icon'd with the {@link ModItems#GOOGLY_EYE googly eye}. It gathers
 * everything the mod adds — the eye, the slimy eye that applies it, and an Optometrist book — so they
 * live in one place rather than scattered across vanilla tabs.
 */
public final class ModCreativeTabs {

    public static final ContentRegistrar.Handle<CreativeModeTab> GOOGLY = new ContentRegistrar.Handle<>();

    private ModCreativeTabs() {
    }

    /** An enchanted book holding {@link ModEnchantments#OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook(HolderLookup.Provider registries) {
        Holder<Enchantment> optometrist = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModEnchantments.OPTOMETRIST);
        return EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(optometrist, 1));
    }

    public static void register(ContentRegistrar registrar) {
        GOOGLY.bind(registrar.registerCreativeTab(
                "googly",
                Component.translatable("itemGroup." + SomeGooglyCommon.MOD_ID),
                () -> new ItemStack(ModItems.GOOGLY_EYE.get()),
                (params, output) -> {
                    output.accept(ModItems.GOOGLY_EYE.get());
                    output.accept(ModItems.SLIMY_EYE.get());
                    output.accept(optometristBook(params.holders()));
                }));
    }
}
