package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extracts frame animation and scopes it to the contained item's draw with vanilla's transforms. */
@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;"
            + "Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("TAIL"))
    private void somegoogly$extractFrameEye(ItemFrame frame, ItemFrameRenderState state, float partialTick,
                                            CallbackInfo callback) {
        PresentationEyes eye = ClientEyeRuntime.frameEye(frame);
        ((PresentationEyes.Holder) state).somegoogly$setFrameEye(
                eye == null ? null : new PresentationEyes.FrameData(eye, partialTick));
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                    + "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private void somegoogly$renderFramedItem(ItemStackRenderState item, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
                                            ItemFrameRenderState state, PoseStack framePose, MultiBufferSource frameBuffer, int frameLight) {
        GooglyEyeItemRenderer.renderInFrame(item, pose, buffer, light, overlay,
                ((PresentationEyes.Holder) state).somegoogly$frameEye());
    }
}
