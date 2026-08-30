package com.github.crittscott.somegoogly.client.compat.neoforge;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.picker.Gizmo;
import com.github.crittscott.somegoogly.client.picker.PickerLayer;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.EyeRenderGating;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

/** Draws normal googly eyes and picker previews at GeckoLib's fully transformed bone poses. */
public class GooglyGeoLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {

    private final ModelGooglyEye modelGooglyEye = new ModelGooglyEye();
    private Frame frame;

    private static final class Frame {
        HeadInfo helper;
        AppearanceOverride overrides;
        GooglyTracker tracker;
        GeoBone[] headBones;
        boolean preview;
        List<PickerState.ListedEye> savedEyes;
        GeoBone[] savedEyeBones;
        GeoBone gizmoBone;
    }

    public GooglyGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                          RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                          float partialTick, int packedLight, int packedOverlay) {
        this.frame = null;
        LivingEntity living = animatable;

        if (PickerState.isActiveTarget(living)) {
            Frame frame = new Frame();
            frame.preview = true;
            frame.savedEyes = PickerState.currentEyes();
            frame.savedEyeBones = new GeoBone[frame.savedEyes.size()];
            for (int i = 0; i < frame.savedEyes.size(); i++) {
                PickerState.ListedEye listed = frame.savedEyes.get(i);
                if (i != PickerState.selectedIndex() && listed.part != null) {
                    frame.savedEyeBones[i] = GeoBones.findBone(bakedModel, listed.part);
                }
            }
            if (PickerState.currentPart() != null) {
                frame.gizmoBone = GeoBones.findBone(bakedModel, PickerState.currentPart());
            }
            this.frame = frame;
            return;
        }

        HeadInfo helper = EyeRenderGating.helperToRender(living);
        if (helper == null) {
            return;
        }

        Frame frame = new Frame();
        frame.helper = helper;
        frame.overrides = EyeState.readProperties(living);
        frame.tracker = ClientEyeRuntime.get(living, helper);
        frame.tracker.markRendered(ClientEyeRuntime.clientTicks());
        frame.headBones = new GeoBone[helper.getHeadCount()];
        for (int h = 0; h < frame.headBones.length; h++) {
            frame.headBones[h] = GeoBones.findBone(bakedModel, helper.getAttachToken(h));
        }
        this.frame = frame;
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        Frame frame = this.frame;
        if (frame == null) {
            return;
        }
        boolean drew = frame.preview
                ? renderPreviewAt(poseStack, bone, frame, bufferSource, packedLight, packedOverlay)
                : renderEyesAt(poseStack, bone, frame, bufferSource, packedLight, packedOverlay, partialTick);
        if (drew) {
            bufferSource.getBuffer(renderType);
        }
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        this.frame = null;
    }

    private boolean renderEyesAt(PoseStack poseStack, GeoBone bone, Frame frame,
                                 MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                 float partialTick) {
        boolean drew = false;
        for (int h = 0; h < frame.headBones.length; h++) {
            if (frame.headBones[h] != bone) {
                continue;
            }
            for (int i = 0; i < frame.helper.getEyeCount(h); i++) {
                if (frame.helper.getEyeScale(h, i) <= 0F) {
                    continue;
                }
                GooglyEyeRenderer.renderEye(poseStack, modelGooglyEye, bufferSource, packedLight,
                        packedOverlay, frame.tracker, frame.helper, frame.overrides, h, i, partialTick);
                drew = true;
            }
        }
        return drew;
    }

    private boolean renderPreviewAt(PoseStack poseStack, GeoBone bone, Frame frame,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean drew = false;
        for (int i = 0; i < frame.savedEyeBones.length; i++) {
            if (frame.savedEyeBones[i] != bone) {
                continue;
            }
            PickerLayer.renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight,
                    packedOverlay, frame.savedEyes.get(i).eye);
            drew = true;
        }
        if (frame.gizmoBone == bone) {
            Gizmo.draw(poseStack, bufferSource);
            if (PickerState.currentEye() != null) {
                PickerLayer.renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight,
                        packedOverlay, PickerState.currentEye());
            }
            drew = true;
        }
        return drew;
    }
}
