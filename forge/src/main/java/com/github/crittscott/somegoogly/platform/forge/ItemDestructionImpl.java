package com.github.crittscott.somegoogly.platform.forge;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

/** Forge implementation of the emptied-item reports, through the item-destroyed event. */
public final class ItemDestructionImpl {

    private ItemDestructionImpl() {}

    /**
     * Forge fires its item-destroyed event from {@code hurtAndBreak}, so reporting here would send
     * it twice.
     */
    public static void reportBroken(Player player, ItemStack original, InteractionHand hand) {}

    public static void reportConsumed(Player player, ItemStack original, InteractionHand hand) {
        ForgeEventFactory.onPlayerDestroyItem(player, original, hand);
    }
}
