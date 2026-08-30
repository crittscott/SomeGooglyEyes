package com.github.crittscott.somegoogly.enchant;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/** Resource keys for the mod's data-driven enchantments. */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> OPTOMETRIST = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "optometrist"));

    private ModEnchantments() {
    }
}
