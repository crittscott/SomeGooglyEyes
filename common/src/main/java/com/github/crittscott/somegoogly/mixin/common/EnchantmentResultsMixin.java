package com.github.crittscott.somegoogly.mixin.common;

import com.github.crittscott.somegoogly.enchant.OptometristEnchantment;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Optometrist off everything but shears and books when loot rolls enchantments by level, as fishing treasure
 * and End City chests do. That roll asks the enchantment's category, not {@code canEnchant}, and Optometrist's
 * category is {@code BREAKABLE}, the closest vanilla one. Fabric has no way to declare a custom category, and a
 * Forge-only category would leave Fabric with a different rule, so both loaders narrow the roll here with
 * {@link OptometristEnchantment#canRollOn}.
 */
@Mixin(EnchantmentHelper.class)
abstract class EnchantmentResultsMixin {

    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
    private static void somegoogly$narrowOptometrist(int level, ItemStack stack, boolean allowTreasure,
                                                     CallbackInfoReturnable<List<EnchantmentInstance>> callback) {
        if (!OptometristEnchantment.canRollOn(stack)) {
            callback.getReturnValue().removeIf(result -> result.enchantment instanceof OptometristEnchantment);
        }
    }
}
