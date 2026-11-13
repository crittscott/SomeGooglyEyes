package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the hotbar's extraction with a fixed hotbar presentation key, so its items' eyes are keyed by vanilla's
 * per-slot item seed rather than by screen position. One Mixin serves every loader; each loader's HUD-layer hook
 * would have to wrap the same extraction separately.
 */
@Mixin(Hud.class)
abstract class HotbarItemEyesMixin {

    @Inject(method = "extractItemHotbar", at = @At("HEAD"))
    private void somegoogly$beginHotbar(CallbackInfo callback) {
        ClientEyeRuntime.beginGuiPresentation(ClientEyeRuntime.HUD, ClientEyeRuntime.GuiKind.HOTBAR, 0);
    }

    @Inject(method = "extractItemHotbar", at = @At("RETURN"))
    private void somegoogly$endHotbar(CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
