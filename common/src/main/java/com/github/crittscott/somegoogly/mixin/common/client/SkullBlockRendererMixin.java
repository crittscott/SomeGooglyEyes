package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import org.jspecify.annotations.Nullable;

/**
 * No loader has a block-entity renderer hook, so placed heads' eyes ride vanilla's head renderer: their eyes are
 * chosen as the head's render state is extracted, and the placed-head submission is bracketed with a
 * {@link ClientEyeRuntime} draw context carrying them for {@link SkullEyeRenderer}, which submits the eyes alongside
 * the skull model.
 */
@Mixin(SkullBlockRenderer.class)
abstract class SkullBlockRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void somegoogly$extractEyes(SkullBlockEntity skull, SkullBlockRenderState state, float partialTick,
                                        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling,
                                        CallbackInfo callback) {
        ((SkullEyeRenderer.PlacedHolder) state).somegoogly$setEyes(SkullEyeRenderer.extract(skull), partialTick);
    }

    // Capture the same model state and pose that the skull submits to the collector.
    @Inject(method = "submitSkull", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel"))
    private static void somegoogly$drawEyes(CallbackInfo callback,
                                            @Local(argsOnly = true) PoseStack pose,
                                            @Local(argsOnly = true) SubmitNodeCollector collector,
                                            @Local(argsOnly = true, ordinal = 0) int packedLight,
                                            @Local(argsOnly = true) SkullModelBase model,
                                            @Local SkullModelBase.State state) {
        SkullEyeRenderer.render(pose, collector, packedLight, model, state);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN"))
    private void somegoogly$endHead(SkullBlockRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void somegoogly$beginHead(SkullBlockRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                      CameraRenderState camera, CallbackInfo callback) {
        SkullEyeRenderer.PlacedHolder placed = (SkullEyeRenderer.PlacedHolder) state;
        ClientEyeRuntime.beginHead(placed.somegoogly$eyes(), placed.somegoogly$partialTick());
    }
}
