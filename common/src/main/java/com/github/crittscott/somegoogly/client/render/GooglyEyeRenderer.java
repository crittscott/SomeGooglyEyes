package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * The mod's one eye-drawing path: mob eyes for both render layers (the vanilla {@link LayerGooglyEyes}
 * and the GeckoLib {@code GooglyGeoLayer}), picker previews, and the 3D eye item all draw through
 * {@link #drawEye}. Keeping the appearance overrides (dye / harvested-eye item / slimy eye / NBT) here
 * means a mob looks the same whether its model is vanilla or GeckoLib.
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
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(
            SomeGooglyCommon.MOD_ID, "textures/model/modelgooglyeye.png");
    public static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    public static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    private GooglyEyeRenderer() {
    }

    // Scratch space reused by captureGravity across every eye, every frame: the render thread is
    // single-threaded and one call finishes before the next starts (mirrors GooglyTracker's static
    // INFLUENCE), so mutating these in place instead of allocating fresh JOML objects is safe.
    private static final Matrix3f GRAVITY_POSE = new Matrix3f();
    private static final Vector3f GRAVITY_DOWN = new Vector3f();
    // Per-eye color scratch for renderEye, under the same single-threaded reasoning.
    private static final float[] CORNEA = new float[3];
    private static final float[] IRIS = new float[3];

    /**
     * Record world-down in this eye's pupil plane for the next physics tick, using the eye's fully
     * composed animated pose. The result is independent of camera orientation and uses the tracker
     * simulation's coordinate convention.
     */
    private static void captureGravity(PoseStack pose, GooglyTracker.EyeInfo eyeInfo) {
        // The entity pose here is local → world (the view/camera rotation lives in RenderSystem's
        // model-view matrix, not the entity pose stack), so world-down maps into the eye's pupil
        // plane straight through the pose's inverse.
        GRAVITY_POSE.set(pose.last().pose());
        GRAVITY_POSE.invert();
        GRAVITY_DOWN.set(0F, -1F, 0F);
        GRAVITY_POSE.transform(GRAVITY_DOWN);
        eyeInfo.gravX = -GRAVITY_DOWN.x;
        eyeInfo.gravY = -GRAVITY_DOWN.y;
    }

    /**
     * Draw a picker draft eye as a static preview (centered iris, no physics) at the current pose, which
     * the caller has already moved into the draft's attachment space. Used by both the vanilla and the
     * GeckoLib eye layers.
     */
    public static void renderPreviewEye(PoseStack pose, ModelGooglyEye model, MultiBufferSource bufferSource,
                                        int packedLight, int overlay, EyeDraft eye) {
        pose.pushPose();
        pose.translate(eye.position[0], eye.position[1], eye.position[2]);
        pose.mulPose(EyePlacement.orientation(eye.inclination, eye.azimuth));
        float scale = eye.eyeScale;
        pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH * eye.depth);
        drawEye(pose, model, bufferSource, packedLight, overlay, eye.corneaColors, eye.irisColors, eye.irisScale,
                0F, 0F, eye.glows);
        pose.popPose();
    }

    /**
     * Draw one eye at the current pose, already scaled to the eye's size: the cornea, the iris scaled by
     * {@code irisScale} and offset to {@code (irisX, irisY)} in the unit disk, and, when {@code glow} is
     * set, the same two parts again in the emissive render type. Every eye the mod draws (mob, picker
     * preview, item) comes through here.
     */
    public static void drawEye(PoseStack pose, ModelGooglyEye model, MultiBufferSource bufferSource,
                               int packedLight, int overlay, float[] cornea, float[] iris, float irisScale,
                               float irisX, float irisY, boolean glow) {
        model.moveIris(irisX, irisY, irisScale);
        drawPass(pose, model, bufferSource.getBuffer(RENDER_TYPE), packedLight, overlay, cornea, iris, irisScale);
        if (glow) {
            drawPass(pose, model, bufferSource.getBuffer(RENDER_TYPE_EYES), packedLight, overlay, cornea, iris,
                    irisScale);
        }
    }

    private static void drawPass(PoseStack pose, ModelGooglyEye model, VertexConsumer buffer, int packedLight,
                                 int overlay, float[] cornea, float[] iris, float irisScale) {
        model.renderCornea(pose, buffer, packedLight, overlay, cornea[0], cornea[1], cornea[2], 1F);
        pose.pushPose();
        pose.scale(irisScale, irisScale, 1F);
        model.renderIris(pose, buffer, packedLight, overlay, iris[0], iris[1], iris[2], 1F);
        pose.popPose();
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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
        Vec3 position = placement.position();
        pose.translate(position.x, position.y, position.z);
        pose.mulPose(placement.orientation());

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

        EyeColor cornea = look.cornea();
        CORNEA[0] = cornea.r();
        CORNEA[1] = cornea.g();
        CORNEA[2] = cornea.b();
        // Color-change behavior blends the cornea toward its target color.
        if (eyeInfo.tintColor != null) {
            float tintAmount = lerp(eyeInfo.prevTintAmount, eyeInfo.tintAmount, partialTicks);
            if (tintAmount > 0F) {
                for (int c = 0; c < 3; c++) {
                    CORNEA[c] = lerp(CORNEA[c], eyeInfo.tintColor[c], tintAmount);
                }
            }
        }
        EyeColor iris = look.iris();
        IRIS[0] = iris.r();
        IRIS[1] = iris.g();
        IRIS[2] = iris.b();

        // Pupil position: the physics delta, which already includes any behavior spring. The simulation
        // keeps it inside the unit disk (the full cornea circle once mapped), so no clamp is needed here.
        float irisX = lerp(eyeInfo.prevDeltaX, eyeInfo.deltaX, partialTicks);
        float irisY = lerp(eyeInfo.prevDeltaY, eyeInfo.deltaY, partialTicks);
        drawEye(pose, model, bufferSource, packedLight, overlay, CORNEA, IRIS, placement.irisScale(),
                irisX, irisY, look.glow());

        pose.popPose();
    }
}
