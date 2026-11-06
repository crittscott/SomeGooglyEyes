package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.GuiEyeAnimations;
import com.github.crittscott.somegoogly.client.GuiItemEyes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Logical slot and carried-item identities survive movement, layout changes, and stack copies. */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerItemEyesMixin {

    @Inject(method = "renderFloatingItem", at = @At("HEAD"))
    private void somegoogly$beginCarried(CallbackInfo callback) {
        GuiItemEyes.beginPresentation(this, GuiEyeAnimations.Kind.CURSOR, 0);
    }

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V", at = @At("HEAD"))
    private void somegoogly$beginSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        GuiItemEyes.beginPresentation(this, GuiEyeAnimations.Kind.SLOT, slot.index);
    }

    @Inject(method = "renderFloatingItem", at = @At("RETURN"))
    private void somegoogly$endCarried(CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V", at = @At("RETURN"))
    private void somegoogly$endSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }
}
