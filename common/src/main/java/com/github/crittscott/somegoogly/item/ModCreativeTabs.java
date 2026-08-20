package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
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

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(SomeGooglyCommon.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> GOOGLY = TABS.register("googly", () ->
            CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("itemGroup." + SomeGooglyCommon.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.GOOGLY_EYE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.GOOGLY_EYE.get());
                        output.accept(ModItems.SLIMY_EYE.get());
                        output.accept(optometristBook());
                    })));

    private ModCreativeTabs() {
    }

    /** An enchanted book holding {@link ModEnchantments#OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook() {
        Enchantment optometrist = ModEnchantments.OPTOMETRIST.get();
        return EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(optometrist, optometrist.getMaxLevel()));
    }

    public static void register() {
        TABS.register();
    }
}
