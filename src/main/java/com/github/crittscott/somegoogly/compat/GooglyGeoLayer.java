package com.github.crittscott.somegoogly.compat;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.picker.Gizmo;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.state.EyeProperties;
import com.github.crittscott.somegoogly.state.EyeState;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
        // While the picker is active, force eyes on every eye-configured mob for authoring (mirrors
        // LayerGooglyEyes); otherwise honour the server's per-mob spawn decision.
        if (!PickerState.active && !living.getPersistentData().getBoolean("somegoogly:hasGooglyEyes")) {
            return;
        }
        HeadInfo helper = HeadInfo.getHelper(entityType, living);
        if (helper == null || !helper.hasConfig()) {
            return;
        }

        // Per-mob appearance overrides (dye / redstone / harvested-eye item / potion), the same as the
        // vanilla layer applies — without this, GeckoLib mobs would ignore item/NBT appearance changes.
        EyeProperties overrides = EyeState.readProperties(living);

        GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        tracker.setLastUpdateRequest();
        tracker.requireUpdate();

        // Keystone B: interpolate the per-mob expression scalars once for this frame (blink, hurt-grow,
        // anger tint, stare, swirl). Shared with the vanilla layer so both render identically.
        GooglyEyeRenderer.Frame frame = GooglyEyeRenderer.Frame.capture(tracker, partialTick);

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
                    GooglyEyeRenderer.renderEye(poseStack, modelGooglyEye, bufferSource, packedLight,
                            packedOverlay, frame, tracker, helper, overrides, h, i, partialTick);
                }
            }
            poseStack.popPose();
        }
    }

    private void renderPickerPreview(PoseStack poseStack, BakedGeoModel bakedModel,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Saved eyes. The selected one is skipped here — it's shown live as the current eye instead.
        for (int i = 0; i < PickerState.eyes.size(); i++) {
            if (i == PickerState.selectedIndex) {
                continue;
            }
            PickerState.ListedEye listed = PickerState.eyes.get(i);
            if (listed.part == null) {
                continue;
            }
            poseStack.pushPose();
            if (GeoBones.moveTo(poseStack, bakedModel, listed.part)) {
                renderPreviewEye(poseStack, bufferSource, packedLight, packedOverlay, listed.eye);
            }
            poseStack.popPose();
        }
        String token = PickerState.currentPart;
        if (token != null) {
            poseStack.pushPose();
            if (GeoBones.moveTo(poseStack, bakedModel, token)) {
                Gizmo.draw(poseStack, bufferSource);
                renderPreviewEye(poseStack, bufferSource, packedLight, packedOverlay, PickerState.currentEye);
            }
            poseStack.popPose();
        }
    }

    private void renderPreviewEye(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, EyeConfig eye) {
        poseStack.pushPose();
        poseStack.translate(eye.position[0] + eye.sideOffset, eye.position[1], eye.position[2]);
        HeadInfo.applyRotation(poseStack, eye);
        float scale = (float) eye.eyeScale;
        poseStack.scale(scale, scale, scale * 0.4F);

        VertexConsumer buf = bufferSource.getBuffer(GooglyEyeRenderer.RENDER_TYPE);
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
