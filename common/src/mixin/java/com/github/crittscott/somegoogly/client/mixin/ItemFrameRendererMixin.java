package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies frame identity only during the contained item's draw, after vanilla's mounting transforms. */
@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMixin {

    @Redirect(method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic("
                    + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;II"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/world/level/Level;I)V"))
    private void somegoogly$renderFramedItem(ItemRenderer renderer, ItemStack stack, ItemDisplayContext context,
                                            int light, int overlay, PoseStack pose, MultiBufferSource buffer,
                                            Level level, int seed, ItemFrame frame, float yaw, float partialTick,
                                            PoseStack framePose, MultiBufferSource frameBuffer, int frameLight) {
        GooglyEyeItemRenderer.renderInFrame(renderer, stack, context, light, overlay, pose, buffer, level, seed,
                frame, partialTick);
    }
}
