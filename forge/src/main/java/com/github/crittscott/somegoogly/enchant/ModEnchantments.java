package com.github.crittscott.somegoogly.enchant;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Enchantment registry for the mod: the shears-only {@link OptometristEnchantment}. */
public final class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, SomeGoogly.MOD_ID);

    public static final RegistryObject<Enchantment> OPTOMETRIST =
            ENCHANTMENTS.register("optometrist", OptometristEnchantment::new);

    private ModEnchantments() {
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). */
    public static void register(IEventBus modBus) {
        ENCHANTMENTS.register(modBus);
    }
}
