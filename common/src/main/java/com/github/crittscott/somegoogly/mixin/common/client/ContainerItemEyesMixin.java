package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets each slot's draw and the carried item's draw with a GUI presentation key, so a slot's or
 * the cursor's eyes keep their state through movement, layout changes, and stack copies. Neither
 * loader has a per-slot or carried-item render event; their container-screen events cover the whole
 * screen.
 */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerItemEyesMixin {

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "renderFloatingItem", at = @At("HEAD"))
    private void somegoogly$beginCarried(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            String text,
            CallbackInfo callback) {
        ClientEyeRuntime.beginGuiPresentation(this, ClientEyeRuntime.GuiKind.CURSOR, 0, stack);
    }

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(
            method =
                    "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at = @At("HEAD"))
    private void somegoogly$beginSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        ClientEyeRuntime.beginGuiPresentation(
                this, ClientEyeRuntime.GuiKind.SLOT, slot.index, slot.getItem());
    }

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "renderFloatingItem", at = @At("RETURN"))
    private void somegoogly$endCarried(CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(
            method =
                    "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at = @At("RETURN"))
    private void somegoogly$endSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
