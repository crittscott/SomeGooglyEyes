package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.client.picker.PickerHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the picker HUD over the rest of the HUD. Forge for this Minecraft version has no HUD layer
 * registration or HUD render event, so the panel follows vanilla's own HUD draw.
 */
@Mixin(Gui.class)
abstract class GuiPickerHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void somegoogly$renderPickerHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        PickerHud.render(graphics, graphics.guiWidth(), graphics.guiHeight());
    }
}
