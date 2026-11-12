package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the frame's animation and identity on its contained item's render state, whose own {@code submit} then
 * scopes it. The Forge and NeoForge frame-item render events fire before the item is submitted with no matching
 * event after it, and Fabric has none, so one hook serves every loader.
 */
@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;"
            + "Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("TAIL"))
    private void somegoogly$extractFrameEye(ItemFrame frame, ItemFrameRenderState state, float partialTick,
                                            CallbackInfo callback) {
        ((PresentationEyes.Holder) state.item).somegoogly$setDraw(
                PresentationEyes.Draw.of(ClientEyeRuntime.frameEye(frame), partialTick));
    }
}
