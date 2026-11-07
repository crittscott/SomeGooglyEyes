package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GuiItemEyes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selects GUI animation before models are resolved; rendering states retain the selection. */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiItemEyesMixin {

    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void somegoogly$beginGuiItem(LivingEntity owner, Level level, ItemStack stack, int x, int y,
                                         int seed, CallbackInfo callback) {
        GuiItemEyes.beginDraw(stack, x, y, seed);
    }

    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
    private void somegoogly$endGuiItem(LivingEntity owner, Level level, ItemStack stack, int x, int y,
                                       int seed, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
