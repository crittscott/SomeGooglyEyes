package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.compat.ExoticBirdsCompat;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Eye layer for vanilla-style {@link LivingEntityRenderer}s. It shares visibility decisions with the
 * GeckoLib layer through {@link EyeRenderGating}, resolves configured attachment tokens through
 * {@link Resolvers}, and delegates each eye's drawing to {@link GooglyEyeRenderer}. Picker previews
 * are drawn by the separate {@link com.github.crittscott.somegoogly.client.picker.PickerLayer}.
 *
 * <p>The GeckoLib counterpart is
 * {@link com.github.crittscott.somegoogly.client.compat.GooglyGeoLayer}, which uses the same gate and
 * eye renderer from GeckoLib's per-bone callback.
 */
public class LayerGooglyEyes<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final ModelGooglyEye modelGooglyEye;

    public LayerGooglyEyes(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T living,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        // While the picker is editing this entity, only its preview layer should draw.
        if (PickerState.isActiveTarget(living)) {
            return;
        }

        // Shared gate (client disables, has-eyes, invisibility, usable config) — see GooglyGeoLayer.
        HeadInfo helper = EyeRenderGating.helperToRender(living);
        if (helper == null) {
            return;
        }

        // Per-mob appearance overrides (dye / redstone / harvested-eye item), layered on top of the
        // shared config below. Same AppearanceOverride an eye item carries.
        AppearanceOverride overrides = EyeState.readProperties(living);

        // Resolve the part-tree strategy for this model family (string names / stable indices).
        M model = this.getParentModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (resolver == null) {
            return;
        }

        GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        tracker.markRendered();

        int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

        int headCount = helper.getHeadCount();
        for (int headIndex = 0; headIndex < headCount; headIndex++) {
            poseStack.pushPose();
            ExoticBirdsCompat.preTransform(model, poseStack);

            // Move into this head's animated space, by the configured string part name.
            if (!resolver.toAttachmentSpace(poseStack, model, helper.getAttachToken(headIndex))) {
                poseStack.popPose();
                continue;
            }

            int eyeCount = helper.getEyeCount(headIndex);
            for (int eyeIndex = 0; eyeIndex < eyeCount; eyeIndex++) {
                if (helper.getEyeScale(headIndex, eyeIndex) <= 0F) {
                    continue;
                }

                GooglyEyeRenderer.renderEye(poseStack, modelGooglyEye, bufferSource, packedLight, overlay,
                        tracker, helper, overrides, headIndex, eyeIndex, partialTicks);
            }

            poseStack.popPose();
        }
    }
}
