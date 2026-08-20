package com.github.crittscott.somegoogly.enchant;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

/** Enchantment registry for the mod: the shears-only {@link OptometristEnchantment}. */
public final class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(SomeGooglyCommon.MOD_ID, Registries.ENCHANTMENT);

    public static final RegistrySupplier<Enchantment> OPTOMETRIST =
            ENCHANTMENTS.register("optometrist", OptometristEnchantment::new);

    private ModEnchantments() {
    }

    public static void register() {
        ENCHANTMENTS.register();
    }
}
