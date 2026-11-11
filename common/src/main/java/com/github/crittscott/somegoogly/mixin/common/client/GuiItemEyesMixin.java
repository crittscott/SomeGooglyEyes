package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Selects GUI animation and position-based identity for every GUI item before its models are resolved; the item's
 * render state keeps the selection until its draw. No loader has a render event around a single GUI item.
 */
@Mixin(GuiGraphics.class)
abstract class GuiItemEyesMixin {

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void somegoogly$beginGuiItem(LivingEntity owner, Level level, ItemStack stack, int x, int y,
                                         int seed, CallbackInfo callback) {
        ClientEyeRuntime.beginGuiDraw(stack, x, y, seed);
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
    private void somegoogly$endGuiItem(LivingEntity owner, Level level, ItemStack stack, int x, int y,
                                       int seed, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
