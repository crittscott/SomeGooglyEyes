package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric hook for completed villager and wandering-trader trades. */
@Mixin(MerchantResultSlot.class)
abstract class MerchantResultSlotMixin {

    @Shadow
    @Final
    private Merchant merchant;

    @Inject(method = "onTake", at = @At("TAIL"))
    private void somegoogly$afterTrade(Player player, ItemStack stack, CallbackInfo callback) {
        if (merchant instanceof AbstractVillager villager && !villager.level().isClientSide()) {
            ServerBehaviorScheduler.onTrade(villager);
        }
    }
}
