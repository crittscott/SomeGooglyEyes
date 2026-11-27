package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;

import net.minecraft.client.gui.Gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the hotbar's draw with a fixed hotbar presentation key, so its items' eyes are keyed by
 * vanilla's per-slot item seed rather than by screen position. Fabric has no hotbar render event,
 * and one Mixin serves both loaders.
 */
@Mixin(Gui.class)
abstract class HotbarItemEyesMixin {

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void somegoogly$beginHotbar(CallbackInfo callback) {
        ClientEyeRuntime.beginGuiPresentation(
                ClientEyeRuntime.HUD, ClientEyeRuntime.GuiKind.HOTBAR, 0, null);
    }

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void somegoogly$endHotbar(CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
