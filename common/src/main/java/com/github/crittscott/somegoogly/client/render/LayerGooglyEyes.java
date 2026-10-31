package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.picker.Gizmo;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.List;

/**
 * Eye layer for vanilla-style {@link LivingEntityRenderer}s. It draws from the {@link EyeRenderData}
 * taken at render-state extraction, which shares its visibility gate with the GeckoLib layer, resolves
 * configured attachment tokens through {@link Resolvers}, and delegates each eye's drawing to
 * {@link GooglyEyeRenderer}.
 *
 * <p>For the picker's target the layer draws the authoring preview instead: the edited variant's saved
 * eyes, the live draft eye, and the selection gizmo, with centered irises (no physics), since placement
 * is what matters there.
 *
 * <p>Vanilla poses its shared entity model only when the submitted model is drawn, after every layer has
 * submitted, so the layer poses the parent model for this render state itself before walking its parts.
 *
 * <p>The GeckoLib counterpart is {@code GooglyGeoLayer}, which makes the same split from GeckoLib's
 * per-bone callback.
 */
public class LayerGooglyEyes<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {
    public LayerGooglyEyes(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, S state,
                       float yRot, float xRot) {
        EyeRenderData data = EyeRenderData.of(state);
        if (!data.pickerTarget() && data.helper() == null) {
            return;
        }

        // Resolve the part-tree strategy for this model family (string names / stable indices).
        M model = this.getParentModel();
        model.setupAnim(state);
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

        if (data.pickerTarget()) {
            renderPreview(poseStack, collector, packedLight, overlay, state, model, resolver);
        } else {
            renderEyes(poseStack, collector, packedLight, overlay, state, model, resolver, data);
        }
    }

    private void renderEyes(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int overlay,
                            S state, M model, EyeAttachmentResolver resolver, EyeRenderData data) {
        HeadInfo helper = data.helper();
        GooglyTracker tracker = data.tracker();
        // Per-mob appearance overrides (dye / redstone / harvested-eye item), layered on top of the
        // shared config below. Same AppearanceOverride an eye item carries; mirrored onto the tracker by
        // ClientNetworkHandler so this doesn't re-parse it from NBT every frame.
        AppearanceOverride overrides = tracker.overrides;
        float partialTicks = data.partialTick();

        int headCount = helper.getHeadCount();
        for (int headIndex = 0; headIndex < headCount; headIndex++) {
            poseStack.pushPose();
            // Move into this head's animated space, by the configured string part name.
            if (resolver.toAttachmentSpace(poseStack, model, helper.getAttachToken(headIndex), state.isBaby)) {
                int eyeCount = helper.getEyeCount(headIndex);
                for (int eyeIndex = 0; eyeIndex < eyeCount; eyeIndex++) {
                    if (helper.getEyeScale(headIndex, eyeIndex) > 0F) {
                        GooglyEyeRenderer.renderEye(poseStack, collector, packedLight, overlay,
                                tracker.eyes[headIndex][eyeIndex], helper, overrides, headIndex, eyeIndex,
                                partialTicks);
                    }
                }
            }
            poseStack.popPose();
        }
    }

    private void renderPreview(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int overlay,
                               S state, M model, EyeAttachmentResolver resolver) {
        // Saved eyes of the variant being edited. The selected one is skipped — shown live as the current eye.
        List<PickerState.ListedEye> eyes = PickerState.currentEyes();
        for (int i = 0; i < eyes.size(); i++) {
            PickerState.ListedEye listed = eyes.get(i);
            if (i == PickerState.selectedIndex() || listed.part == null) {
                continue;
            }
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, listed.part, state.isBaby)) {
                GooglyEyeRenderer.renderPreviewEye(poseStack, collector, packedLight, overlay, listed.eye);
            }
            poseStack.popPose();
        }

        // Gizmo on the active placement part, plus the live draft eye — but only when one is being
        // shaped. With no draft (the empty state) the mob shows just its saved eyes, or nothing at all.
        String token = PickerState.currentPart();
        if (token != null) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, token, state.isBaby)) {
                Gizmo.draw(poseStack, collector);
                if (PickerState.currentEye() != null) {
                    GooglyEyeRenderer.renderPreviewEye(poseStack, collector, packedLight, overlay,
                            PickerState.currentEye());
                }
            }
            poseStack.popPose();
        }
    }
}
