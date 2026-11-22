package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Reports a player's item that a use emptied, for uses that end the click outside the vanilla code that reports
 * it. A used-up stack and a broken tool are reported separately because the loaders' own reporting of the second
 * differs between versions. Forge tells other mods through its item-destroyed event; Fabric has no such event.
 */
public final class ItemDestruction {

    private ItemDestruction() {
    }

    /** {@code player} broke the tool {@code original}, a copy of the stack, in {@code hand} by wearing it out. */
    @ExpectPlatform
    public static void reportBroken(Player player, ItemStack original, InteractionHand hand) {
        throw new AssertionError();
    }

    /** {@code player} used up the last of {@code original}, a copy of the stack, in {@code hand}. */
    @ExpectPlatform
    public static void reportConsumed(Player player, ItemStack original, InteractionHand hand) {
        throw new AssertionError();
    }
}
