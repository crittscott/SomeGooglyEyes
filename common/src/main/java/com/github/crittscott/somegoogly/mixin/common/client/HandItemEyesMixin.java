package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.HandItemEyes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries the actual holder and hand into the immediate item draw, including other players and mobs. */
@Mixin(ItemInHandRenderer.class)
abstract class HandItemEyesMixin {

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void somegoogly$beginHand(LivingEntity holder, ItemStack stack, ItemDisplayContext context,
                                      boolean leftHand, PoseStack pose, MultiBufferSource buffers,
                                      int light, CallbackInfo callback) {
        HandItemEyes.begin(HandItemEyes.context(holder, stack, context));
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void somegoogly$endHand(LivingEntity holder, ItemStack stack, ItemDisplayContext context,
                                    boolean leftHand, PoseStack pose, MultiBufferSource buffers,
                                    int light, CallbackInfo callback) {
        HandItemEyes.end();
    }
}
