package com.github.crittscott.somegoogly.platform.neoforge;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;

/** NeoForge implementation of the broken-item report, through the item-destroyed event. */
public final class ItemDestructionImpl {

    private ItemDestructionImpl() {
    }

    public static void report(Player player, ItemStack original, InteractionHand hand) {
        EventHooks.onPlayerDestroyItem(player, original, hand);
    }
}
