package com.github.crittscott.somegoogly.server.mixin;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports a cow or goat being milked with a bucket, which no loader has an event for. Filling the bucket is
 * the one place each {@code mobInteract} calls {@code ItemUtils.createFilledResult}, so it marks a
 * successful milking. Mooshrooms and modded cows reach it through {@code AbstractCow#mobInteract}.
 */
@Mixin({AbstractCow.class, Goat.class})
abstract class MilkingMixin {

    @Inject(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private void somegoogly$onMilked(Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResult> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            ServerBehaviorScheduler.onMilked(self);
        }
    }
}
