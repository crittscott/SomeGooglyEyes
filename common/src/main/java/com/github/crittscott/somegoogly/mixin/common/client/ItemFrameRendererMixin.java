package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supplies frame identity only during the contained item's draw, after vanilla's mounting transforms. */
@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMixin {

    private static final String DRAW_ITEM = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic("
            + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;II"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/level/Level;I)V";
    private static final String RENDER = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = DRAW_ITEM))
    private void somegoogly$beginFrame(ItemFrame frame, float yaw, float partialTick, PoseStack pose,
                                       MultiBufferSource buffer, int light, CallbackInfo callback) {
        ClientEyeRuntime.beginFrame(frame, partialTick);
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = DRAW_ITEM, shift = At.Shift.AFTER))
    private void somegoogly$endFrame(ItemFrame frame, float yaw, float partialTick, PoseStack pose,
                                     MultiBufferSource buffer, int light, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
