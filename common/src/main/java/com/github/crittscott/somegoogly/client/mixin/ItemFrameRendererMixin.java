package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                    + "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void somegoogly$renderFramedItem(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector buffer, int light, int overlay, int outlineColor,
                                            ItemFrameRenderState state, PoseStack framePose, SubmitNodeCollector frameBuffer, CameraRenderState camera) {
        GooglyEyeItemRenderer.renderInFrame(item, pose, buffer, light, overlay, outlineColor,
                ((PresentationEyes.Holder) state).somegoogly$frameEye());
    }
}
