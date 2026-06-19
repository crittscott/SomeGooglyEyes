package com.github.crittscott.somegoogly.enchant;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * "Optometrist" — a shears-only enchantment that enables non-lethal eye harvesting: right-clicking an
 * eyed mob with optometrist shears pulls its googly eyes off without harming it (see
 * {@code EyeItemInteractions}). Without it, plain shears can only collect eyes by killing the mob.
 *
 * <p>Treasure-only (so it never appears in the enchanting table) but left discoverable and tradeable,
 * matching the vanilla Mending/Frost-Walker pattern: it shows up in loot-chest / fishing enchanted
 * books and librarian trades without needing any loot-table or trade datapack edits.
 */
public class OptometristEnchantment extends Enchantment {

    /** Custom category restricting the enchantment to vanilla shears. */
    public static final EnchantmentCategory SHEARS_CATEGORY =
            EnchantmentCategory.create("somegoogly_shears", item -> item == Items.SHEARS);

    public OptometristEnchantment() {
        super(Rarity.RARE, SHEARS_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public int getMinCost(int level) {
        return 15;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }
}
