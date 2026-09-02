package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.compat.AlexsMobsCompat;
import com.github.crittscott.somegoogly.client.compat.ExoticBirdsCompat;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.client.render.EyeRenderTransforms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Picker preview layer: renders the committed eyes plus the live draft eye and the selection gizmo,
 * but <b>only</b> for the entity the picker is locked onto. Eyes are previewed with a centered iris
 * (no physics) — placement is what matters here. The real {@code LayerGooglyEyes} is suppressed for
 * this entity while the picker is active, so you see only the work-in-progress.
 */
public class PickerLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private final ModelGooglyEye modelGooglyEye;

    public PickerLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T living,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!PickerState.isActiveTarget(living)) {
            return;
        }
        M model = this.getParentModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (resolver == null) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

        // Saved eyes of the variant being edited. The selected one is skipped — shown live as the current eye.
        List<PickerState.ListedEye> eyes = PickerState.currentEyes();
        for (int i = 0; i < eyes.size(); i++) {
            if (i == PickerState.selectedIndex()) {
                continue;
            }
            PickerState.ListedEye listed = eyes.get(i);
            if (listed.part == null) {
                continue;
            }
            poseStack.pushPose();
            ExoticBirdsCompat.preTransform(model, poseStack);
            AlexsMobsCompat.preTransform(model, poseStack);
            if (resolver.toAttachmentSpace(poseStack, model, listed.part)) {
                renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight, overlay, listed.eye);
            }
            poseStack.popPose();
        }

        // Gizmo on the active placement part, plus the live draft eye — but only when one is being
        // shaped. With no draft (the empty state) the mob shows just its saved eyes, or nothing at all.
        String token = PickerState.currentPart();
        if (token != null) {
            poseStack.pushPose();
            ExoticBirdsCompat.preTransform(model, poseStack);
            AlexsMobsCompat.preTransform(model, poseStack);
            if (resolver.toAttachmentSpace(poseStack, model, token)) {
                Gizmo.draw(poseStack, bufferSource);
                if (PickerState.currentEye() != null) {
                    renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight, overlay, PickerState.currentEye());
                }
            }
            poseStack.popPose();
        }
    }

    /**
     * Draw a draft eye as a static preview (centered iris, no physics) at the current pose — placement is
     * what matters when authoring. Shared with the GeckoLib preview path ({@code GooglyGeoLayer}) so the
     * two can't drift; the caller has already moved the pose into the attachment space.
     */
    public static void renderPreviewEye(PoseStack poseStack, ModelGooglyEye model, MultiBufferSource bufferSource,
                                        int packedLight, int overlay, EyeDraft eye) {
        poseStack.pushPose();
        poseStack.translate(eye.position[0], eye.position[1], eye.position[2]);
        EyeRenderTransforms.applyRotation(poseStack, eye.inclination, eye.azimuth);

        float scale = eye.eyeScale;
        poseStack.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH * eye.depth);

        VertexConsumer buffer = bufferSource.getBuffer(GooglyEyeRenderer.RENDER_TYPE);
        float[] cornea = eye.corneaColors;
        model.renderCornea(poseStack, buffer, packedLight, overlay, cornea[0], cornea[1], cornea[2], 1.0F);

        float irisScale = eye.irisScale;
        float[] iris = eye.irisColors;
        poseStack.pushPose();
        poseStack.scale(irisScale, irisScale, 1.0F);
        model.moveIris(0.0F, 0.0F, irisScale); // centered preview, no physics
        model.renderIris(poseStack, buffer, packedLight, overlay, iris[0], iris[1], iris[2], 1.0F);
        poseStack.popPose();

        poseStack.popPose();
    }
}
