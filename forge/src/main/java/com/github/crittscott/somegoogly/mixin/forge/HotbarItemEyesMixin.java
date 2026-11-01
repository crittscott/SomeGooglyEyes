package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.client.GuiEyeAnimations;
import com.github.crittscott.somegoogly.client.GuiItemEyes;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The vanilla hotbar supplies a stable per-slot item seed, independent of screen position. */
@Mixin(Gui.class)
abstract class HotbarItemEyesMixin {

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void somegoogly$beginHotbar(CallbackInfo callback) {
        GuiItemEyes.beginPresentation(GuiEyeAnimations.HUD, GuiEyeAnimations.HOTBAR, 0);
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void somegoogly$endHotbar(CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }
}
