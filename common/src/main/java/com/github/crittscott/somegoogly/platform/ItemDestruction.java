package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Reports an item a player broke, for uses that end the click outside the vanilla code that reports it. Forge tells
 * other mods through its item-destroyed event; Fabric has no such event.
 */
public final class ItemDestruction {

    private ItemDestruction() {
    }

    /** {@code player} broke {@code original}, a copy of the stack, in {@code hand}. */
    @ExpectPlatform
    public static void report(Player player, ItemStack original, InteractionHand hand) {
        throw new AssertionError();
    }
}
