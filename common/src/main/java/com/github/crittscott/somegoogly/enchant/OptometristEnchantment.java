package com.github.crittscott.somegoogly.enchant;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * "Optometrist" — a shears-only enchantment that enables non-lethal eye harvesting: right-clicking an
 * eyed mob with optometrist shears pulls its googly eyes off without harming it (see
 * {@code EyeItemService}). Without it, plain shears can only collect eyes by killing the mob.
 *
 * <p>Treasure-only (so it never appears in the enchanting table) but left discoverable and tradeable,
 * matching the vanilla Mending/Frost-Walker pattern: it shows up in loot-chest / fishing enchanted
 * books and librarian trades without needing any loot-table or trade datapack edits.
 *
 * <p>The closest closed vanilla category is {@link EnchantmentCategory#BREAKABLE};
 * {@link #canEnchant(ItemStack)} narrows that category to shears on both loaders.
 */
public class OptometristEnchantment extends Enchantment {

    public OptometristEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.BREAKABLE,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    @Override
    public int getMinCost(int level) {
        return 15;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof ShearsItem;
    }
}
