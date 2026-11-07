package com.github.crittscott.somegoogly.platform.forge;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Forge fires its item-destroyed event from {@code hurtAndBreak}, so reporting here would send it twice. */
public final class ItemDestructionImpl {

    private ItemDestructionImpl() {
    }

    public static void report(Player player, ItemStack original, InteractionHand hand) {
    }
}
