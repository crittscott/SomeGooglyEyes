package com.github.crittscott.somegoogly.mixin.common.client;

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
        GuiItemEyes.beginPresentation(GuiItemEyes.HUD, GuiItemEyes.Kind.HOTBAR, 0);
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void somegoogly$endHotbar(CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }
}
