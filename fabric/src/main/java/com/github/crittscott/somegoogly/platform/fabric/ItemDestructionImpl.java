package com.github.crittscott.somegoogly.platform.fabric;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Fabric has no item-destroyed event, so there is no one to report to. */
public final class ItemDestructionImpl {

    private ItemDestructionImpl() {}

    public static void reportBroken(Player player, ItemStack original, InteractionHand hand) {}

    public static void reportConsumed(Player player, ItemStack original, InteractionHand hand) {}
}
