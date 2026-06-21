package com.github.crittscott.somegoogly.render;

import com.github.crittscott.somegoogly.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.behavior.EyeRenderContribution;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.state.EyeProperties;
import com.github.crittscott.somegoogly.state.EyeState;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared eye drawing used by both render layers — the vanilla {@link LayerGooglyEyes} and the
 * GeckoLib {@code GooglyGeoLayer}. Keeping the appearance overrides (dye / harvested-eye item /
 * potion / NBT) and the Keystone&nbsp;B behaviour composition here means the two layers can't drift
 * apart: a mob looks the same whether its model is vanilla or GeckoLib.
 *
 * <p>Each eye is drawn from a <b>baseline</b> — physics-driven iris wobble, configured scale, effective
 * cornea colour — with the mob's single active {@link com.github.crittscott.somegoogly.behavior.EyeBehavior}
 * (if any) folded on top via its {@link EyeRenderContribution}. There's at most one behaviour at a time,
 * so no multi-behaviour composition is needed.
 *
 * <p>The caller is responsible only for moving the pose into the head's animated attachment space;
 * this class handles everything from the eye's own offset/rotation inward.
 */
public final class GooglyEyeRenderer {

    private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    public static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    public static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    // Reused per eye on the single render thread, so behaviour composition allocates nothing per frame.
    private static final EyeRenderContribution CONTRIBUTION = new EyeRenderContribution();

    private GooglyEyeRenderer() {
    }

    /**
     * Draw a single eye, applying both the per-mob {@code overrides} and the mob's active behaviour.
     * The pose is expected to already sit in the head's attachment space; this method applies the eye's
     * own offset/rotation/scale within its own push/pop, so it leaves the pose stack as it found it.
     */
    public static void renderEye(PoseStack pose, ModelGooglyEye model, MultiBufferSource bufferSource,
                                 int packedLight, int overlay, GooglyTracker tracker, HeadInfo helper,
                                 EyeProperties overrides, int headIndex, int eyeIndex, float partialTicks) {
        pose.pushPose();

        // Eye offset in head-local coordinates, then the eye's own aim (not the head's) about its centre.
        float[] eyes = helper.getEyeOffsetFromJoint(headIndex, eyeIndex);
        pose.translate(eyes[0] + helper.getEyeSideOffset(headIndex, eyeIndex), eyes[1], eyes[2]);
        HeadInfo.applyRotation(pose, helper.getInclination(headIndex, eyeIndex), helper.getAzimuth(headIndex, eyeIndex));

        GooglyTracker.EyeInfo eyeInfo = tracker.eyes[headIndex][eyeIndex];

        // The active behaviour's per-eye contribution over the neutral baseline (or just the baseline).
        EyeRenderContribution c = CONTRIBUTION;
        c.reset();
        BehaviorInstance active = tracker.active;
        if (active != null) {
            active.behavior.contribute(active, helper, headIndex, eyeIndex, partialTicks, c);
        }

        // Grow scales the whole eye; blink squashes it vertically.
        float eyeScale = helper.getEyeScale(headIndex, eyeIndex) * c.eyeScaleMul;
        pose.scale(eyeScale, eyeScale * c.squashY, eyeScale * 0.4F);

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);

        float[] corneaColours = overrides.corneaColor().isPresent()
                ? EyeState.unpackColor(overrides.corneaColor().getAsInt())
                : helper.getCorneaColours(headIndex, eyeIndex);
        // Colour-change behaviour blends the cornea toward its target colour.
        if (c.corneaTint != null && c.tintAmount > 0F) {
            corneaColours = lerpColor(corneaColours, c.corneaTint, c.tintAmount);
        }
        model.renderCornea(pose, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

        float[] irisColours = overrides.irisColor().isPresent()
                ? EyeState.unpackColor(overrides.irisColor().getAsInt())
                : helper.getIrisColours(headIndex, eyeIndex);
        float irisScale = helper.getIrisScale(headIndex, eyeIndex);

        pose.pushPose();
        pose.scale(irisScale, irisScale, 1F);

        // Iris position: physics wobble pulled toward the behaviour's target by its weight (0 = pure
        // wobble for behaviours that don't drive the pupil; 1 = the behaviour fully owns it).
        float physicsX = lerp(eyeInfo.prevDeltaX, eyeInfo.deltaX, partialTicks);
        float physicsY = lerp(eyeInfo.prevDeltaY, eyeInfo.deltaY, partialTicks);
        float irisX = physicsX * (1F - c.irisWeight) + c.irisTargetX * c.irisWeight;
        float irisY = physicsY * (1F - c.irisWeight) + c.irisTargetY * c.irisWeight;
        model.moveIris(irisX, irisY, irisScale);
        model.renderIris(pose, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
        pose.popPose();

        boolean glow = overrides.glow().isPresent()
                ? overrides.glow().get()
                : helper.doesEyeGlow(headIndex, eyeIndex);
        if (glow) {
            buffer = bufferSource.getBuffer(RENDER_TYPE_EYES);
            model.renderCornea(pose, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

            pose.pushPose();
            pose.scale(irisScale, irisScale, 1F);
            model.renderIris(pose, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
            pose.popPose();
        }

        pose.popPose();
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float[] lerpColor(float[] from, float[] to, float t) {
        return new float[]{lerp(from[0], to[0], t), lerp(from[1], to[1], t), lerp(from[2], to[2], t)};
    }
}
