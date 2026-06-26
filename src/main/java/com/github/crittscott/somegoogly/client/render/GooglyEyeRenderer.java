package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeRenderContribution;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared eye drawing used by both render layers — the vanilla {@link LayerGooglyEyes} and the
 * GeckoLib {@code GooglyGeoLayer}. Keeping the appearance overrides (dye / harvested-eye item /
 * potion / NBT) and the Keystone&nbsp;B behavior composition here means the two layers can't drift
 * apart: a mob looks the same whether its model is vanilla or GeckoLib.
 *
 * <p>Each eye is drawn from a <b>baseline</b> — physics-driven iris wobble, configured scale, effective
 * cornea color — with the mob's single active {@link EyeBehavior}
 * (if any) folded on top via its {@link EyeRenderContribution}. There's at most one behavior at a time,
 * so no multi-behavior composition is needed.
 *
 * <p>The caller is responsible only for moving the pose into the head's animated attachment space;
 * this class handles everything from the eye's own offset/rotation inward.
 */
public final class GooglyEyeRenderer {

    // Reused per eye on the single render thread, so behavior composition allocates nothing per frame.
    private static final EyeRenderContribution CONTRIBUTION = new EyeRenderContribution();

    // TEX precedes the RENDER_TYPE constants by necessity: their initializers read it by simple name,
    // and a forward reference there is a compile error (JLS 8.3.3).
    private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    public static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    public static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    private GooglyEyeRenderer() {
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float[] lerpColor(float[] from, float[] to, float t) {
        return new float[]{lerp(from[0], to[0], t), lerp(from[1], to[1], t), lerp(from[2], to[2], t)};
    }

    /**
     * Draw a single eye, applying both the per-mob {@code overrides} and the mob's active behavior.
     * The pose is expected to already sit in the head's attachment space; this method applies the eye's
     * own offset/rotation/scale within its own push/pop, so it leaves the pose stack as it found it.
     */
    public static void renderEye(PoseStack pose, ModelGooglyEye model, MultiBufferSource bufferSource,
                                 int packedLight, int overlay, GooglyTracker tracker, HeadInfo helper,
                                 AppearanceOverride overrides, int headIndex, int eyeIndex, float partialTicks) {
        pose.pushPose();

        // Placement (geometry) from config; effective appearance is config with the per-mob override on top.
        EyePlacement placement = helper.placementAt(headIndex, eyeIndex);
        EyeAppearance look = helper.appearanceAt(headIndex, eyeIndex).overlay(overrides);

        // Eye offset in head-local coordinates, then the eye's own aim (not the head's) about its center.
        float[] eyes = placement.positionArray();
        pose.translate(eyes[0], eyes[1], eyes[2]);
        HeadInfo.applyRotation(pose, placement);

        GooglyTracker.EyeInfo eyeInfo = tracker.eyes[headIndex][eyeIndex];

        // The active behavior's per-eye contribution over the neutral baseline (or just the baseline).
        EyeRenderContribution c = CONTRIBUTION;
        c.reset();
        BehaviorInstance active = tracker.active;
        if (active != null) {
            active.behavior.contribute(active, helper, headIndex, eyeIndex, partialTicks, c);
        }

        // Grow scales the whole eye; blink squashes it vertically.
        float eyeScale = (float) placement.eyeScale() * c.eyeScaleMul;
        pose.scale(eyeScale, eyeScale * c.squashY, eyeScale * 0.4F);

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);

        float[] corneaColors = look.cornea().toArray();
        // Color-change behavior blends the cornea toward its target color.
        if (c.corneaTint != null && c.tintAmount > 0F) {
            corneaColors = lerpColor(corneaColors, c.corneaTint, c.tintAmount);
        }
        model.renderCornea(pose, buffer, packedLight, overlay, corneaColors[0], corneaColors[1], corneaColors[2], 1F);

        float[] irisColors = look.iris().toArray();
        float irisScale = (float) placement.irisScale();

        pose.pushPose();
        pose.scale(irisScale, irisScale, 1F);

        // Iris position: physics wobble pulled toward the behavior's target by its weight (0 = pure
        // wobble for behaviors that don't drive the pupil; 1 = the behavior fully owns it).
        float physicsX = lerp(eyeInfo.prevDeltaX, eyeInfo.deltaX, partialTicks);
        float physicsY = lerp(eyeInfo.prevDeltaY, eyeInfo.deltaY, partialTicks);
        float irisX = physicsX * (1F - c.irisWeight) + c.irisTargetX * c.irisWeight;
        float irisY = physicsY * (1F - c.irisWeight) + c.irisTargetY * c.irisWeight;
        // The unit disk maps to the full cornea circle; clamp so blended behavior targets can't push
        // the iris past the rim.
        float m2 = irisX * irisX + irisY * irisY;
        if (m2 > 1F) {
            float m = (float) Math.sqrt(m2);
            irisX /= m;
            irisY /= m;
        }
        model.moveIris(irisX, irisY, irisScale);
        model.renderIris(pose, buffer, packedLight, overlay, irisColors[0], irisColors[1], irisColors[2], 1F);
        pose.popPose();

        boolean glow = look.glow();
        if (glow) {
            buffer = bufferSource.getBuffer(RENDER_TYPE_EYES);
            model.renderCornea(pose, buffer, packedLight, overlay, corneaColors[0], corneaColors[1], corneaColors[2], 1F);

            pose.pushPose();
            pose.scale(irisScale, irisScale, 1F);
            model.renderIris(pose, buffer, packedLight, overlay, irisColors[0], irisColors[1], irisColors[2], 1F);
            pose.popPose();
        }

        pose.popPose();
    }
}
