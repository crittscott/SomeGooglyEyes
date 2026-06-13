package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.render.resolver.Resolvers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Picker preview layer: renders the committed eyes plus the live draft eye and the selection gizmo,
 * but <b>only</b> for the entity the picker is locked onto. Eyes are previewed with a centred iris
 * (no physics) — placement is what matters here. The real {@code LayerGooglyEyes} is suppressed for
 * this entity while the picker is active, so you see only the work-in-progress.
 */
public class PickerLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);

    private final ModelGooglyEye modelGooglyEye;

    public PickerLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye(Minecraft.getInstance().getEntityModels().bakeLayer(SomeGoogly.GOOGLY_EYE_LAYER));
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

        // Committed heads/eyes.
        for (HeadConfig head : PickerState.heads.values()) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, head.attachPoint) && head.eyes != null) {
                for (EyeConfig eye : head.eyes) {
                    renderEye(poseStack, bufferSource, packedLight, overlay, eye);
                }
            }
            poseStack.popPose();
        }

        // Draft eye + gizmo on the currently selected part.
        String token = PickerState.selectedToken();
        if (token != null) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, token)) {
                Gizmo.draw(poseStack, bufferSource);
                renderEye(poseStack, bufferSource, packedLight, overlay, PickerState.draft);
            }
            poseStack.popPose();
        }
    }

    private void renderEye(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, EyeConfig eye) {
        poseStack.pushPose();
        poseStack.translate(eye.position[0] + eye.sideOffset, eye.position[1], eye.position[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) eye.yRotation));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) eye.xRotation));

        float scale = (float) eye.eyeScale;
        poseStack.scale(scale, scale, scale * 0.4F);

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);
        double[] cornea = eye.corneaColors;
        modelGooglyEye.renderCornea(poseStack, buffer, packedLight, overlay, (float) cornea[0], (float) cornea[1], (float) cornea[2], 1.0F);

        float irisScale = (float) eye.irisScale;
        double[] iris = eye.irisColors;
        poseStack.pushPose();
        poseStack.scale(irisScale, irisScale, 1.0F);
        modelGooglyEye.moveIris(0.0F, 0.0F, irisScale); // centred preview, no physics
        modelGooglyEye.renderIris(poseStack, buffer, packedLight, overlay, (float) iris[0], (float) iris[1], (float) iris[2], 1.0F);
        poseStack.popPose();

        poseStack.popPose();
    }
}
