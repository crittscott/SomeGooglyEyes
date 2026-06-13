package com.github.crittscott.somegoogly.render;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class LayerGooglyEyes<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEX_GOOGLY_EYE = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX_GOOGLY_EYE);
    private static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX_GOOGLY_EYE);
    private final ModelGooglyEye modelGooglyEye;

    public LayerGooglyEyes(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye(Minecraft.getInstance().getEntityModels().bakeLayer(SomeGoogly.GOOGLY_EYE_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T living, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        // While the picker is editing this entity, only its preview layer should draw.
        if (PickerState.isActiveTarget(living)) {
            return;
        }

        // Client global disable check
        if (ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            return;
        }

        // Client entity-specific disable check
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        for (String s : ClientConfig.DISABLED_ENTITIES.get()) {
            if (new ResourceLocation(s).equals(entityType)) {
                return;
            }
        }

        // Check server decision from NBT data
        if (!living.getPersistentData().getBoolean("somegoogly:hasGooglyEyes")) {
            return;
        }

        HeadInfo helper = HeadInfo.getHelper(entityType);
        if (helper == null || !helper.hasConfig()) {
            return;
        }

        // Resolve the part-tree strategy for this model family (string names / stable indices).
        M model = this.getParentModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (resolver == null) {
            return;
        }

        GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        tracker.setLastUpdateRequest();
        tracker.requireUpdate();

        int headCount = helper.getHeadCount();
        for (int headIndex = 0; headIndex < headCount; headIndex++) {
            poseStack.pushPose();

            // Move into this head's animated space, by the configured string part name.
            if (!resolver.toAttachmentSpace(poseStack, model, helper.getAttachToken(headIndex))) {
                poseStack.popPose();
                continue;
            }

            int eyeCount = helper.getEyeCount(headIndex);
            for (int eyeIndex = 0; eyeIndex < eyeCount; eyeIndex++) {
                if (living.isInvisible() && helper.affectedByInvisibility(headIndex, eyeIndex)) {
                    continue;
                }

                float eyeScale = helper.getEyeScale(headIndex, eyeIndex);
                if (eyeScale <= 0F) {
                    continue;
                }

                poseStack.pushPose();

                // Apply eye offset in head-local coordinates
                float[] eyes = helper.getEyeOffsetFromJoint(headIndex, eyeIndex);
                poseStack.translate(eyes[0] + helper.getEyeSideOffset(headIndex, eyeIndex), eyes[1], eyes[2]);

                // Apply individual eye rotations (for eye direction, not head orientation)
                poseStack.mulPose(Axis.YP.rotationDegrees(helper.getEyeRotation(headIndex, eyeIndex)));
                poseStack.mulPose(Axis.XP.rotationDegrees(helper.getEyeTopRotation(headIndex, eyeIndex)));

                poseStack.scale(eyeScale, eyeScale, eyeScale * 0.4F);

                VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);
                int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

                float[] corneaColours = helper.getCorneaColours(headIndex, eyeIndex);
                modelGooglyEye.renderCornea(poseStack, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

                float[] irisColours = helper.getIrisColours(headIndex, eyeIndex);
                float irisScale = helper.getIrisScale(headIndex, eyeIndex);

                GooglyTracker.EyeInfo eyeInfo = tracker.eyes[headIndex][eyeIndex];
                poseStack.pushPose();
                poseStack.scale(irisScale, irisScale, 1F);

                // Apply physics simulation to iris position - this is the googly eye effect
                modelGooglyEye.moveIris(
                        eyeInfo.prevDeltaX + (eyeInfo.deltaX - eyeInfo.prevDeltaX) * partialTicks,
                        eyeInfo.prevDeltaY + (eyeInfo.deltaY - eyeInfo.prevDeltaY) * partialTicks,
                        irisScale
                );

                modelGooglyEye.renderIris(poseStack, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
                poseStack.popPose();

                if (helper.doesEyeGlow(headIndex, eyeIndex)) {
                    buffer = bufferSource.getBuffer(RENDER_TYPE_EYES);
                    modelGooglyEye.renderCornea(poseStack, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

                    poseStack.pushPose();
                    poseStack.scale(irisScale, irisScale, 1F);
                    modelGooglyEye.renderIris(poseStack, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
                    poseStack.popPose();
                }

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }
}
