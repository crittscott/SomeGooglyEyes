package com.github.crittscott.somegoogly.mixin.neoforge.twilight.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chooses a placed skull candle's eyes as its render state is extracted and brackets the candle's {@code submit} with
 * a {@link ClientEyeRuntime} draw context carrying them, so the skull model submitted inside gets them as a vanilla
 * head's does under {@code SkullBlockRendererMixin}. Twilight Forest is an optional mod with no API for this, and its
 * skull candle renderer and block entity are its own, so vanilla's head renderer never extracts or submits them.
 */
@Pseudo
@Mixin(targets = "twilightforest.client.renderer.block.SkullCandleRenderer", remap = false)
abstract class TwilightSkullCandleRendererMixin {
    private static final String STATE = "Ltwilightforest/client/state/block/SkullCandleRenderState;";
    private static final String SUBMIT = "submit(" + STATE + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method = "extractRenderState(Ltwilightforest/block/entity/SkullCandleBlockEntity;" + STATE
            + "FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void somegoogly$extractEyes(@Coerce BlockEntity head, @Coerce Object state, float partialTick,
                                        Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling,
                                        CallbackInfo callback) {
        ((SkullEyeRenderer.PlacedHolder) state).somegoogly$setEyes(SkullEyeRenderer.extract(head), partialTick);
    }

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void somegoogly$begin(@Coerce Object state, com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.SubmitNodeCollector collector,
                                 net.minecraft.client.renderer.state.level.CameraRenderState camera,
                                 CallbackInfo callback) {
        SkullEyeRenderer.PlacedHolder placed = (SkullEyeRenderer.PlacedHolder) state;
        ClientEyeRuntime.beginHead(placed.somegoogly$eyes(), placed.somegoogly$partialTick());
    }

    @Inject(method = SUBMIT, at = @At("RETURN"))
    private void somegoogly$end(CallbackInfo callback) {
        ClientEyeRuntime.endItemDraw();
    }
}
