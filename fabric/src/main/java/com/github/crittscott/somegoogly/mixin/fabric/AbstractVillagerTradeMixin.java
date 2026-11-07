package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric hook for completed villager and wandering-trader trades, where NeoForge and Forge post {@code TradeWithVillagerEvent}. */
@Mixin(AbstractVillager.class)
abstract class AbstractVillagerTradeMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void somegoogly$afterTrade(MerchantOffer offer, CallbackInfo callback) {
        ServerBehaviorScheduler.onTrade((AbstractVillager) (Object) this);
    }
}
