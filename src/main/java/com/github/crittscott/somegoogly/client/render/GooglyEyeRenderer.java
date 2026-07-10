package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Shared eye drawing used by both render layers — the vanilla {@link LayerGooglyEyes} and the
 * GeckoLib {@code GooglyGeoLayer}. Keeping the appearance overrides (dye / harvested-eye item /
 * potion / NBT) here means the two layers can't drift apart: a mob looks the same whether its model is
 * vanilla or GeckoLib.
 *
 * <p>This class only <b>renders</b>: the active behavior was already folded into the eye's simulated
 * state by {@link GooglyTracker} (pupil physics + spring, plus the grow/blink/color overlays), so here
 * we just read that per-eye state, interpolate it by {@code partialTicks}, and draw. There is no
 * behavior logic on the render thread.
 *
 * <p>The caller is responsible only for moving the pose into the head's animated attachment space;
 * this class handles everything from the eye's own offset/rotation inward.
 */
public final class GooglyEyeRenderer {

    // TEX precedes the RENDER_TYPE constants by necessity: their initializers read it by simple name,
    // and a forward reference there is a compile error (JLS 8.3.3).
    private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    public static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    public static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    private GooglyEyeRenderer() {
    }

    /**
     * Read which way world-down points in this eye's pupil plane and stash it for the next physics tick,
     * so the pupil rests at true down regardless of how the eye is aimed/animated/oriented.
     *
     * <p>The pose here maps pupil-local → <i>view</i> space (entity rendering bakes the camera rotation
     * into the pose stack). Rather than reconstruct the camera (whose yaw/180°/order conventions are easy
     * to get wrong), we compose the engine's own {@code view → world} rotation
     * ({@link RenderSystem#getInverseViewRotationMatrix()}, derived from the same pose) with the local →
     * view pose to get local → world, then invert it to map world-down into the pupil plane. The camera
     * cancels by construction, so gravity doesn't depend on where you look. The stored
     * {@code (gravX, gravY)} is in the physics {@code (deltaX, deltaY)} convention, which is the negation
     * of the model geometry ({@link com.github.crittscott.somegoogly.client.ModelGooglyEye#moveIris}
     * renders at {@code -norm}), hence the trailing minus signs.
     */
    private static void captureGravity(PoseStack pose, GooglyTracker.EyeInfo eyeInfo) {
        // localToWorld = (view → world) · (local → view)
        Matrix3f localToWorld = new Matrix3f(RenderSystem.getInverseViewRotationMatrix())
                .mul(new Matrix3f(pose.last().pose()));
        Vector3f down = localToWorld.invert().transform(new Vector3f(0F, -1F, 0F));
        eyeInfo.gravX = -down.x;
        eyeInfo.gravY = -down.y;
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

        // The pose now sits in this eye's pupil plane (post offset + aim, pre-scale): capture which way
        // world-down points here, for the next tick's gravity. This is the one spot that knows the eye's
        // fully-animated orientation.
        captureGravity(pose, eyeInfo);

        // Everything below reads the eye's already-simulated state (the behavior, if any, was folded in
        // by the tracker) and interpolates by partialTicks. Grow scales the whole eye; blink squashes it.
        float eyeScaleMul = lerp(eyeInfo.prevScaleMul, eyeInfo.scaleMul, partialTicks);
        float squashY = lerp(eyeInfo.prevSquashY, eyeInfo.squashY, partialTicks);
        float eyeScale = placement.eyeScale() * eyeScaleMul;
        pose.scale(eyeScale, eyeScale * squashY,
                eyeScale * ModelGooglyEye.BASE_DEPTH * placement.depth());

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);

        float[] corneaColors = look.cornea().toArray();
        // Color-change behavior blends the cornea toward its target color.
        if (eyeInfo.tintColor != null) {
            float tintAmount = lerp(eyeInfo.prevTintAmount, eyeInfo.tintAmount, partialTicks);
            if (tintAmount > 0F) {
                corneaColors = lerpColor(corneaColors, eyeInfo.tintColor, tintAmount);
            }
        }
        model.renderCornea(pose, buffer, packedLight, overlay, corneaColors[0], corneaColors[1], corneaColors[2], 1F);

        float[] irisColors = look.iris().toArray();
        float irisScale = placement.irisScale();

        pose.pushPose();
        pose.scale(irisScale, irisScale, 1F);

        // Pupil position: the physics delta, which already includes any behavior spring. The simulation
        // keeps it inside the unit disk (the full cornea circle once mapped), so no clamp is needed here.
        float irisX = lerp(eyeInfo.prevDeltaX, eyeInfo.deltaX, partialTicks);
        float irisY = lerp(eyeInfo.prevDeltaY, eyeInfo.deltaY, partialTicks);
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
