package com.github.crittscott.somegoogly.render;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class LayerGooglyEyes<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEX_GOOGLY_EYE = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX_GOOGLY_EYE);
    private static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX_GOOGLY_EYE);
    private final ModelGooglyEye modelGooglyEye;
    private static final Marker MARK = MarkerManager.getMarker(LayerGooglyEyes.class.getSimpleName());

    public LayerGooglyEyes(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye(Minecraft.getInstance().getEntityModels().bakeLayer(SomeGoogly.GOOGLY_EYE_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity living, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        // Client global disable check
        if (ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            return;
        }

        // Client entity-specific disable check
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        for (String s : ClientConfig.DISABLED_ENTITIES.get()) {
            ResourceLocation disabled = new ResourceLocation(s);
            if (disabled.equals(entityType)) {
                return;
            }
        }

        // Check server decision from NBT data
        boolean hasGooglyEyes = living.getPersistentData().getBoolean("somegoogly:hasGooglyEyes");
        if (!hasGooglyEyes) {
            return;
        }
//SomeGoogly.LOGGER.debug(MARK,"has googly eyes for {}",living.getType());

        HeadInfo parentHelper = HeadInfo.getHelper(entityType);
        if (parentHelper != null) {
            EntityRenderer<?> render = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
            if (!(render instanceof LivingEntityRenderer)) {
                return;
            }
            LivingEntityRenderer<?, ?> renderer = (LivingEntityRenderer<?, ?>) render;

            if (!parentHelper.setup(living, renderer)) {
                return;
            }

            GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, parentHelper);
            tracker.setLastUpdateRequest();
            tracker.requireUpdate();

            int headCount = parentHelper.getHeadCount(living);

            for (int headIndex = 0; headIndex < headCount; headIndex++) {
                HeadInfo helper = parentHelper.getHeadInfo(living, headIndex);

                if (helper.noFaceInfo) {
                    continue;
                }

                helper.setHeadModel(living, renderer);
                if (helper.headModel == null) {
                    continue;
                }

                poseStack.pushPose();

                // Transform to head coordinate space - after this, we're working in head-local coordinates
                helper.correctPosition(living, poseStack, partialTicks);

                int eyeCount = helper.getEyeCount(living);

                for (int eyeIndex = 0; eyeIndex < eyeCount; eyeIndex++) {
                    if (living.isInvisible() && helper.affectedByInvisibility(living, eyeIndex)) {
                        continue;
                    }

                    float eyeScale = helper.getEyeScale(living, poseStack, partialTicks, eyeIndex);

                    if (eyeScale <= 0F) {
                        continue;
                    }

                    poseStack.pushPose();

                    helper.preChildEntHeadRenderCalls(living, poseStack, renderer);

                    // Apply eye offset in head-local coordinates
                    float[] eyes = helper.getEyeOffsetFromJoint(living, poseStack, partialTicks, eyeIndex);
                    poseStack.translate(eyes[0] + helper.getEyeSideOffset(living, poseStack, partialTicks, eyeIndex), eyes[1], eyes[2]);

                    // Apply individual eye rotations (for eye direction, not head orientation)
                    poseStack.mulPose(Axis.YP.rotationDegrees(helper.getEyeRotation(living, poseStack, partialTicks, eyeIndex)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(helper.getEyeTopRotation(living, poseStack, partialTicks, eyeIndex)));

                    poseStack.scale(eyeScale, eyeScale, eyeScale * 0.4F);

                    VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);

                    int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

                    float[] corneaColours = helper.getCorneaColours(living, poseStack, partialTicks, eyeIndex);
                    modelGooglyEye.renderCornea(poseStack, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

                    float[] irisColours = helper.getIrisColours(living, poseStack, partialTicks, eyeIndex);

                    float irisScale = helper.getIrisScale(living, poseStack, partialTicks, eyeIndex);
                    poseStack.pushPose();
                    poseStack.scale(irisScale, irisScale, 1F);

                    // Apply physics simulation to iris position - this is the googly eye effect
                    modelGooglyEye.moveIris(
                            tracker.eyes[headIndex][eyeIndex].prevDeltaX + (tracker.eyes[headIndex][eyeIndex].deltaX - tracker.eyes[headIndex][eyeIndex].prevDeltaX) * partialTicks,
                            tracker.eyes[headIndex][eyeIndex].prevDeltaY + (tracker.eyes[headIndex][eyeIndex].deltaY - tracker.eyes[headIndex][eyeIndex].prevDeltaY) * partialTicks,
                            irisScale
                    );

                    modelGooglyEye.renderIris(poseStack, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
                    poseStack.popPose();

                    if (helper.doesEyeGlow(living, eyeIndex)) {
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
}