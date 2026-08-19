package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.picker.Gizmo;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.picker.PickerLayer;
import com.github.crittscott.somegoogly.client.render.EyeRenderGating;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

/**
 * GeckoLib render layer that draws googly eyes on named bones (the GeckoLib counterpart of
 * {@code LayerGooglyEyes} + {@code PickerLayer}). Handles both normal rendering (from synced config,
 * with iris physics) and the picker preview when this entity is the picker's target.
 *
 * <p>The layer works through GeckoLib's <b>per-bone</b> hook, not the whole-model {@link #render}
 * callback: GeckoLib applies the entity's body rotations and model transforms inside its own
 * push/pop and has already popped them by the time {@code render} runs, so only
 * {@link #renderForBone} sees a pose actually positioned at the animated bone. Per-entity gating and
 * bone lookup happen once in {@link #preRender}; {@link #renderForBone} draws on the matching bones;
 * {@link #render} (which GeckoLib calls last) just clears the per-render state.
 *
 * <p>Only loaded when GeckoLib is present (referenced via {@link GeckoCompat}).
 */
public class GooglyGeoLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {

    private final ModelGooglyEye modelGooglyEye;

    /**
     * State computed in {@link #preRender} for the entity currently being rendered, consumed by
     * {@link #renderForBone}, cleared in {@link #render}. The layer is a per-renderer singleton, but
     * entity rendering is single-threaded and the preRender → renderForBone → render sequence is not
     * interleaved between entities, so one transient slot is safe. {@code null} means "draw nothing".
     */
    private Frame frame;

    /** Either a picker preview ({@code preview} true) or normal synced-config rendering. */
    private static final class Frame {
        // Normal rendering: config + per-mob state, with each head's attach token resolved to a bone
        // (null where the token matched no bone in the model currently worn — see Ribbits model swaps).
        HeadInfo helper;
        AppearanceOverride overrides;
        GooglyTracker tracker;
        GeoBone[] headBones;

        // Picker preview: the edited variant's saved eyes (the selected one stays null — it is shown
        // live as the current eye), plus the bone carrying the gizmo and the live eye.
        boolean preview;
        List<PickerState.ListedEye> savedEyes;
        GeoBone[] savedEyeBones;
        GeoBone gizmoBone;
    }

    public GooglyGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye();
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                          MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                          int packedLight, int packedOverlay) {
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

        // Shared gate (client disables, has-eyes, invisibility, usable config) — see LayerGooglyEyes.
        HeadInfo helper = EyeRenderGating.helperToRender(living);
        if (helper == null) {
            return;
        }

        Frame frame = new Frame();
        frame.helper = helper;
        // Per-mob appearance overrides (dye / redstone / harvested-eye item / slimy eye), the same as the
        // vanilla layer applies — without this, GeckoLib mobs would ignore item/NBT appearance changes.
        frame.overrides = EyeState.readProperties(living);
        frame.tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        frame.tracker.markRendered(ClientEventHandler.clientTicks());
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
            // renderForBone contract: we rendered into our own RenderTypes, so restore the model's buffer.
            bufferSource.getBuffer(renderType);
        }
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        // All bones have rendered; drop the per-render state so no entity/tracker refs outlive the frame.
        this.frame = null;
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
                        packedOverlay, frame.tracker, frame.helper, frame.overrides, h, i, partialTick);
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
            PickerLayer.renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight, packedOverlay,
                    frame.savedEyes.get(i).eye);
            drew = true;
        }
        if (frame.gizmoBone == bone) {
            Gizmo.draw(poseStack, bufferSource);
            if (PickerState.currentEye() != null) {
                PickerLayer.renderPreviewEye(poseStack, modelGooglyEye, bufferSource, packedLight, packedOverlay,
                        PickerState.currentEye());
            }
            drew = true;
        }
        return drew;
    }
}
