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
 * Brackets every GUI item draw, giving the item's eyes their GUI animation and position-based identity. Neither
 * loader has a render event around a single GUI item.
 */
@Mixin(GuiGraphics.class)
abstract class GuiItemEyesMixin {

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("HEAD"))
    private void somegoogly$beginGuiItem(LivingEntity owner, Level level, ItemStack stack, int x, int y,
                                         int seed, int z, CallbackInfo callback) {
        ClientEyeRuntime.beginGuiDraw(stack, x, y, seed);
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("RETURN"))
    private void somegoogly$endGuiItem(LivingEntity owner, Level level, ItemStack stack, int x, int y,
                                       int seed, int z, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
