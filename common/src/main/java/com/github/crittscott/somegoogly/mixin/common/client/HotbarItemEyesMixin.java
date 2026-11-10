package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.GuiItemEyes;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the hotbar's draw with a fixed hotbar presentation key, so its items' eyes are keyed by vanilla's
 * per-slot item seed rather than by screen position. Fabric has no hotbar render event, so one Mixin serves every
 * loader.
 */
@Mixin(Gui.class)
abstract class HotbarItemEyesMixin {

    @Inject(method = "renderItemHotbar", at = @At("HEAD"))
    private void somegoogly$beginHotbar(CallbackInfo callback) {
        GuiItemEyes.beginPresentation(GuiItemEyes.HUD, GuiItemEyes.Kind.HOTBAR, 0);
    }

    @Inject(method = "renderItemHotbar", at = @At("RETURN"))
    private void somegoogly$endHotbar(CallbackInfo callback) {
        GuiItemEyes.endPresentation();
    }
}
