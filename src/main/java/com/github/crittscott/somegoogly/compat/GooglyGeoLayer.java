package com.github.crittscott.somegoogly.compat;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import com.github.crittscott.somegoogly.head.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.picker.Gizmo;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib render layer that draws googly eyes on named bones (the GeckoLib counterpart of
 * {@code LayerGooglyEyes} + {@code PickerLayer}). Handles both normal rendering (from synced config,
 * with iris physics) and the picker preview when this entity is the picker's target.
 *
 * <p>Only loaded when GeckoLib is present (referenced via {@link GeckoCompat}).
 */
public class GooglyGeoLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    private static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    private final ModelGooglyEye modelGooglyEye;

    public GooglyGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye(Minecraft.getInstance().getEntityModels().bakeLayer(SomeGoogly.GOOGLY_EYE_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        LivingEntity living = animatable;

        if (PickerState.isActiveTarget(living)) {
            renderPickerPreview(poseStack, bakedModel, bufferSource, packedLight, packedOverlay);
            return;
        }

        if (ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            return;
        }
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (ClientConfig.isEntityDisabled(entityType)) {
            return;
        }
        if (!living.getPersistentData().getBoolean("somegoogly:hasGooglyEyes")) {
            return;
        }
        HeadInfo helper = HeadInfo.getHelper(entityType, living);
        if (helper == null || !helper.hasConfig()) {
            return;
        }

        GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        tracker.setLastUpdateRequest();
        tracker.requireUpdate();

        int headCount = helper.getHeadCount();
        for (int h = 0; h < headCount; h++) {
            poseStack.pushPose();
            if (GeoBones.moveTo(poseStack, bakedModel, helper.getAttachToken(h))) {
                int eyeCount = helper.getEyeCount(h);
                for (int i = 0; i < eyeCount; i++) {
                    if (living.isInvisible() && helper.affectedByInvisibility(h, i)) {
                        continue;
                    }
                    if (helper.getEyeScale(h, i) <= 0F) {
                        continue;
                    }
                    GooglyTracker.EyeInfo eye = tracker.eyes[h][i];
                    float dx = eye.prevDeltaX + (eye.deltaX - eye.prevDeltaX) * partialTick;
                    float dy = eye.prevDeltaY + (eye.deltaY - eye.prevDeltaY) * partialTick;
                    renderEye(poseStack, bufferSource, packedLight, packedOverlay, living, helper, h, i, dx, dy);
                }
            }
            poseStack.popPose();
        }
    }

    private void renderPickerPreview(PoseStack poseStack, BakedGeoModel bakedModel,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (HeadConfig head : PickerState.heads.values()) {
            poseStack.pushPose();
            if (GeoBones.moveTo(poseStack, bakedModel, head.attachPoint) && head.eyes != null) {
                for (EyeConfig eye : head.eyes) {
                    renderPreviewEye(poseStack, bufferSource, packedLight, packedOverlay, eye);
                }
            }
            poseStack.popPose();
        }
        String token = PickerState.selectedToken();
        if (token != null) {
            poseStack.pushPose();
            if (GeoBones.moveTo(poseStack, bakedModel, token)) {
                Gizmo.draw(poseStack, bufferSource);
                renderPreviewEye(poseStack, bufferSource, packedLight, packedOverlay, PickerState.draft);
            }
            poseStack.popPose();
        }
    }

    private void renderEye(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                           LivingEntity living, HeadInfo helper, int h, int i, float irisDx, float irisDy) {
        poseStack.pushPose();
        float[] off = helper.getEyeOffsetFromJoint(h, i);
        poseStack.translate(off[0] + helper.getEyeSideOffset(h, i), off[1], off[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees(helper.getEyeRotation(h, i)));
        poseStack.mulPose(Axis.XP.rotationDegrees(helper.getEyeTopRotation(h, i)));
        float scale = helper.getEyeScale(h, i);
        poseStack.scale(scale, scale, scale * 0.4F);

        VertexConsumer buf = bufferSource.getBuffer(RENDER_TYPE);
        float[] cornea = helper.getCorneaColours(h, i);
        modelGooglyEye.renderCornea(poseStack, buf, packedLight, packedOverlay, cornea[0], cornea[1], cornea[2], 1F);

        float[] iris = helper.getIrisColours(h, i);
        float irisScale = helper.getIrisScale(h, i);
        poseStack.pushPose();
        poseStack.scale(irisScale, irisScale, 1F);
        modelGooglyEye.moveIris(irisDx, irisDy, irisScale);
        modelGooglyEye.renderIris(poseStack, buf, packedLight, packedOverlay, iris[0], iris[1], iris[2], 1F);
        poseStack.popPose();

        if (helper.doesEyeGlow(h, i)) {
            VertexConsumer glow = bufferSource.getBuffer(RENDER_TYPE_EYES);
            modelGooglyEye.renderCornea(poseStack, glow, packedLight, packedOverlay, cornea[0], cornea[1], cornea[2], 1F);
            poseStack.pushPose();
            poseStack.scale(irisScale, irisScale, 1F);
            modelGooglyEye.renderIris(poseStack, glow, packedLight, packedOverlay, iris[0], iris[1], iris[2], 1F);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void renderPreviewEye(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, EyeConfig eye) {
        poseStack.pushPose();
        poseStack.translate(eye.position[0] + eye.sideOffset, eye.position[1], eye.position[2]);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) eye.yRotation));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) eye.xRotation));
        float scale = (float) eye.eyeScale;
        poseStack.scale(scale, scale, scale * 0.4F);

        VertexConsumer buf = bufferSource.getBuffer(RENDER_TYPE);
        double[] cornea = eye.corneaColors;
        modelGooglyEye.renderCornea(poseStack, buf, packedLight, packedOverlay, (float) cornea[0], (float) cornea[1], (float) cornea[2], 1F);

        float irisScale = (float) eye.irisScale;
        double[] iris = eye.irisColors;
        poseStack.pushPose();
        poseStack.scale(irisScale, irisScale, 1F);
        modelGooglyEye.moveIris(0F, 0F, irisScale);
        modelGooglyEye.renderIris(poseStack, buf, packedLight, packedOverlay, (float) iris[0], (float) iris[1], (float) iris[2], 1F);
        poseStack.popPose();

        poseStack.popPose();
    }
}
