package com.github.crittscott.somegoogly.client.compat.gecko;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.client.picker.Gizmo;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.EyeRenderData;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.PerBoneRender;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Captures eye and picker data during GeckoLib render-state extraction, then registers drawing tasks
 * at the animated attachment bones. Loaded only when GeckoLib is present.
 */
public final class GooglyGeoLayer<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState>
        extends GeoRenderLayer<T, Void, R> {

    private static final DataTicket<Frame> FRAME = DataTicket.create("somegoogly:eyes", Frame.class);

    private final ModelGooglyEye modelGooglyEye = new ModelGooglyEye();

    public GooglyGeoLayer(GeoRenderer<T, Void, R> renderer) {
        super(renderer);
    }

    /** Data belongs to one extracted render state, never to the renderer's shared layer. */
    private static final class Frame {
        EyeDraft currentEye;
        String currentPart;
        GeoBone gizmoBone;
        GeoBone[] headBones;
        HeadInfo helper;
        AppearanceOverride overrides;
        float partialTick;
        boolean preview;
        GeoBone[] savedEyeBones;
        List<PickerState.ListedEye> savedEyes;
        GooglyTracker tracker;
    }

    @Override
    public void addPerBoneRender(R renderState, BakedGeoModel bakedModel,
                                 BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        Frame frame = renderState.getGeckolibData(FRAME);
        if (frame == null) {
            return;
        }
        Set<GeoBone> bones = new LinkedHashSet<>();
        if (frame.preview) {
            frame.savedEyeBones = new GeoBone[frame.savedEyes.size()];
            for (int i = 0; i < frame.savedEyes.size(); i++) {
                String part = frame.savedEyes.get(i).part;
                if (part != null) {
                    frame.savedEyeBones[i] = GeoBones.findBone(bakedModel, part);
                    bones.add(frame.savedEyeBones[i]);
                }
            }
            if (frame.currentPart != null) {
                frame.gizmoBone = GeoBones.findBone(bakedModel, frame.currentPart);
                bones.add(frame.gizmoBone);
            }
        } else {
            frame.headBones = new GeoBone[frame.helper.getHeadCount()];
            for (int h = 0; h < frame.headBones.length; h++) {
                frame.headBones[h] = GeoBones.findBone(bakedModel, frame.helper.getAttachToken(h));
                bones.add(frame.headBones[h]);
            }
        }
        bones.remove(null);
        for (GeoBone bone : bones) {
            consumer.accept(bone, (state, pose, attachment, renderType, buffers, light, overlay, color) -> {
                boolean drew = frame.preview
                        ? renderPreviewAt(pose, attachment, frame, buffers, light, overlay)
                        : renderEyesAt(pose, attachment, frame, buffers, light, overlay, frame.partialTick);
                if (drew && renderType != null) {
                    buffers.getBuffer(renderType);
                }
            });
        }
    }

    @Override
    public void addRenderData(T animatable, Void relatedObject, R renderState) {
        Frame frame = null;
        if (animatable instanceof LivingEntity living) {
            float partialTick = renderState.getGeckolibData(DataTickets.PARTIAL_TICK);
            EyeRenderData data = EyeRenderData.extract(living, partialTick);
            if (data.pickerTarget()) {
                frame = new Frame();
                frame.preview = true;
                frame.currentPart = PickerState.currentPart();
                frame.currentEye = PickerState.currentEye() == null ? null : PickerState.currentEye().copy();
                frame.savedEyes = new ArrayList<>();
                List<PickerState.ListedEye> eyes = PickerState.currentEyes();
                for (int i = 0; i < eyes.size(); i++) {
                    if (i != PickerState.selectedIndex()) {
                        PickerState.ListedEye eye = eyes.get(i);
                        frame.savedEyes.add(new PickerState.ListedEye(eye.part, eye.eye.copy()));
                    }
                }
            } else if (data.helper() != null) {
                frame = new Frame();
                frame.helper = data.helper();
                frame.tracker = data.tracker();
                frame.overrides = frame.tracker.overrides;
                frame.partialTick = partialTick;
            }
        }
        renderState.addGeckolibData(FRAME, frame);
    }

    private boolean renderEyesAt(PoseStack poseStack, GeoBone bone, Frame frame, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay, float partialTick) {
        boolean drew = false;
        for (int h = 0; h < frame.headBones.length; h++) {
            if (frame.headBones[h] != bone) {
                continue;
            }
            int eyeCount = frame.helper.getEyeCount(h);
            for (int i = 0; i < eyeCount; i++) {
                if (frame.helper.getEyeScale(h, i) <= 0F) {
                    continue;
                }
                GooglyEyeRenderer.renderEye(poseStack, modelGooglyEye, bufferSource, packedLight,
                        packedOverlay, frame.tracker.eyes[h][i], frame.helper, frame.overrides, h, i, partialTick);
                drew = true;
            }
        }
        return drew;
    }

    private boolean renderPreviewAt(PoseStack poseStack, GeoBone bone, Frame frame, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        boolean drew = false;
        for (int i = 0; i < frame.savedEyeBones.length; i++) {
            if (frame.savedEyeBones[i] != bone) {
                continue;
            }
            GooglyEyeRenderer.renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight, packedOverlay,
                    frame.savedEyes.get(i).eye);
            drew = true;
        }
        if (frame.gizmoBone == bone) {
            Gizmo.draw(poseStack, bufferSource);
            if (frame.currentEye != null) {
                GooglyEyeRenderer.renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight, packedOverlay,
                        frame.currentEye);
            }
            drew = true;
        }
        return drew;
    }
}
