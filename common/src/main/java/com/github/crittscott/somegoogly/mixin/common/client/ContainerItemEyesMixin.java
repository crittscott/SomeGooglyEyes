package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets each slot's extraction and the carried item's extraction with a GUI presentation key, so a slot's or the
 * cursor's eyes keep their state through movement, layout changes, and stack copies. No loader has a per-slot or
 * carried-item render event; their container-screen events cover the whole screen.
 */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerItemEyesMixin {

    @Inject(method = "extractFloatingItem", at = @At("HEAD"))
    private void somegoogly$beginCarried(CallbackInfo callback) {
        ClientEyeRuntime.beginGuiPresentation(this, ClientEyeRuntime.GuiKind.CURSOR, 0);
    }

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V", at = @At("HEAD"))
    private void somegoogly$beginSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo callback) {
        ClientEyeRuntime.beginGuiPresentation(this, ClientEyeRuntime.GuiKind.SLOT, slot.index);
    }

    @Inject(method = "extractFloatingItem", at = @At("RETURN"))
    private void somegoogly$endCarried(CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V", at = @At("RETURN"))
    private void somegoogly$endSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
