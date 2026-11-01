package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.GuiEyeAnimations;
import com.github.crittscott.somegoogly.client.GuiItemEyes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Logical slot and carried-item identities survive movement, layout changes, and stack copies. */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerItemEyesMixin {

    @Inject(method = "extractFloatingItem", at = @At("HEAD"))
    private void somegoogly$beginCarried(CallbackInfo callback) {
        GuiItemEyes.beginPresentation(this, GuiEyeAnimations.CURSOR, 0);
    }

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V", at = @At("HEAD"))
    private void somegoogly$beginSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo callback) {
        GuiItemEyes.beginPresentation(this, GuiEyeAnimations.SLOT, slot.index);
    }

    @Inject(method = "extractFloatingItem", at = @At("RETURN"))
    private void somegoogly$endCarried(CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V", at = @At("RETURN"))
    private void somegoogly$endSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }
}
