package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.client.picker.Gizmo;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.eye.EyeAppearance;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

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
    private static final ResourceLocation TEX = SomeGooglyCommon.id("textures/model/modelgooglyeye.png");
    public static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    public static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    private static final Matrix3f GRAVITY_POSE = new Matrix3f();
    // Reused on the render thread; each eye finishes its draw before the next one starts.
    private static final float[] CORNEA = new float[3];
    private static final float[] IRIS = new float[3];
    // One instance whose iris is moved before each draw on the render thread.
    private static final ModelGooglyEye MODEL = new ModelGooglyEye();
    /** The pupil of an eye drawn without a simulation. */
    private static final GooglyTracker.EyeInfo REST = rest();

    private GooglyEyeRenderer() {
    }

    /**
     * Capture the eye plane for the next physics tick after composing the eye pose and before
     * expression scaling. The pupil's gravity mode selects world-down or GUI screen-down and
     * determines whether display scale is removed. Only the first draw after a tick captures.
     */
    public static void captureGravity(PoseStack pose, GooglyTracker.EyeInfo eyeInfo) {
        if (!eyeInfo.poseStale) {
            return;
        }
        GRAVITY_POSE.set(pose.last().pose());
        eyeInfo.capturePose(GRAVITY_POSE, eyeInfo.guiGravity ? null
                : Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
    }

    /**
     * Draw one simulated eye at the current pose, which sits at the eye's center in its pupil plane, at its
     * configured {@code eyeScale}, {@code depth} multiplier, and pupil size {@code irisScale}. Reads the
     * eye's already-simulated state (the behavior, if any, was folded in by its simulator) and interpolates
     * it by {@code partialTicks}: grow scales the whole eye, blink and the squints squash it, the tints blend
     * its colors, love and the creepy pulse swell the pupil, and a sleeping villager's lids are drawn over it
     * last. Leaves the pose stack as it found it.
     */
    public static void drawAnimatedEye(PoseStack pose, MultiBufferSource bufferSource,
                                       int packedLight, int overlay, GooglyTracker.EyeInfo eyeInfo,
                                       float eyeScale, float depth, EyeColor cornea, EyeColor iris,
                                       float irisScale, boolean glow, float partialTicks) {
        pose.pushPose();
        float eyeScaleMul = Mth.lerp(partialTicks, eyeInfo.prevScaleMul, eyeInfo.scaleMul);
        float squashY = Mth.lerp(partialTicks, eyeInfo.prevSquashY, eyeInfo.squashY);
        float scale = eyeScale * eyeScaleMul;
        pose.scale(scale, scale * squashY, scale * ModelGooglyEye.BASE_DEPTH * depth);

        CORNEA[0] = cornea.r();
        CORNEA[1] = cornea.g();
        CORNEA[2] = cornea.b();
        // Color-change behavior blends the cornea toward its target color.
        if (eyeInfo.tintColor != null) {
            float tintAmount = Mth.lerp(partialTicks, eyeInfo.prevTintAmount, eyeInfo.tintAmount);
            if (tintAmount > 0F) {
                for (int c = 0; c < 3; c++) {
                    CORNEA[c] = Mth.lerp(tintAmount, CORNEA[c], eyeInfo.tintColor[c]);
                }
            }
        }
        IRIS[0] = iris.r();
        IRIS[1] = iris.g();
        IRIS[2] = iris.b();
        // Anger blends the pupil toward red.
        float irisTintAmount = Mth.lerp(partialTicks, eyeInfo.prevIrisTintAmount, eyeInfo.irisTintAmount);
        if (irisTintAmount > 0F) {
            for (int c = 0; c < 3; c++) {
                IRIS[c] = Mth.lerp(irisTintAmount, IRIS[c], GooglyTracker.ANGER_IRIS_COLOR[c]);
            }
        }

        // Pupil position: the physics delta, which already includes any behavior spring. The simulation
        // keeps it inside the unit disk (the full cornea circle once mapped), so no clamp is needed here.
        float irisX = Mth.lerp(partialTicks, eyeInfo.prevDeltaX, eyeInfo.deltaX);
        float irisY = Mth.lerp(partialTicks, eyeInfo.prevDeltaY, eyeInfo.deltaY);
        // Love and the creepy pulse swell the pupil, capped where it fills the cornea; the cap never shrinks a
        // configured size.
        float irisScaleMul = Mth.lerp(partialTicks, eyeInfo.prevIrisScaleMul, eyeInfo.irisScaleMul);
        float pupilScale = Math.min(irisScale * irisScaleMul, Math.max(irisScale, ModelGooglyEye.MAX_PUPIL_SIZE));
        drawEye(pose, bufferSource, packedLight, overlay, CORNEA, IRIS, pupilScale, irisX, irisY, glow);

        // A sleeping villager's lids close over the eye from above and below. They never glow: drawn only in
        // the cutout pass, they hide the glow behind them by depth.
        float lid = Mth.lerp(partialTicks, eyeInfo.prevLid, eyeInfo.lid);
        if (lid > 0F) {
            float[] lidColor = GooglyTracker.SLEEP_LID_COLOR;
            MODEL.renderLids(pose, bufferSource.getBuffer(RENDER_TYPE), packedLight, overlay,
                    lidColor[0], lidColor[1], lidColor[2], 1F, lid);
        }

        pose.popPose();
    }

    /**
     * Draw one eye at the current pose, already scaled to the eye's size: the cornea, the iris scaled by
     * {@code irisScale} and offset to {@code (irisX, irisY)} in the unit disk, and, when {@code glow} is
     * set, the same two parts again in the emissive render type. Every eye the mod draws (mob, picker
     * preview, item) comes through here.
     */
    public static void drawEye(PoseStack pose, MultiBufferSource bufferSource,
                               int packedLight, int overlay, float[] cornea, float[] iris, float irisScale,
                               float irisX, float irisY, boolean glow) {
        MODEL.moveIris(irisX, irisY, irisScale);
        drawPass(pose, bufferSource.getBuffer(RENDER_TYPE), packedLight, overlay, cornea, iris, irisScale);
        if (glow) {
            drawPass(pose, bufferSource.getBuffer(RENDER_TYPE_EYES), packedLight, overlay, cornea, iris, irisScale);
        }
    }

    private static void drawPass(PoseStack pose, VertexConsumer buffer, int packedLight,
                                 int overlay, float[] cornea, float[] iris, float irisScale) {
        MODEL.renderCornea(pose, buffer, packedLight, overlay, cornea[0], cornea[1], cornea[2], 1F);
        pose.pushPose();
        pose.scale(irisScale, irisScale, 1F);
        MODEL.renderIris(pose, buffer, packedLight, overlay, iris[0], iris[1], iris[2], 1F);
        pose.popPose();
    }

    /**
     * Draw {@code boat}'s eyes in the hull model's space, which already includes vanilla yaw, damage rocking, and
     * bubble rotation. The caller retains the hull pose; ordinary boat rendering and water masking stay with vanilla.
     */
    public static void renderBoat(Boat boat, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        PresentationEyes eyes = ClientEyeRuntime.boatEyes(boat);
        if (eyes == null) {
            return;
        }
        for (int part : eyes.parts) {
            renderHead(pose, buffers, light, OverlayTexture.NO_OVERLAY, eyes.eyes[part], eyes.helper,
                    eyes.snapshot.properties(), part, partialTick);
        }
    }

    /**
     * Draw a single eye, simulated by {@code eyeInfo}, applying both the per-mob (or per-head) {@code overrides}
     * and the active behavior. The pose is expected to already sit in the head's attachment space; this method
     * applies the eye's own offset/rotation/scale within its own push/pop, so it leaves the pose stack as it
     * found it.
     */
    private static void renderEye(PoseStack pose, MultiBufferSource bufferSource,
                                 int packedLight, int overlay, GooglyTracker.EyeInfo eyeInfo, HeadInfo helper,
                                 AppearanceOverride overrides, int headIndex, int eyeIndex, float partialTicks) {
        pose.pushPose();

        // Placement (geometry) from config; effective appearance is config with the per-mob override on top.
        EyePlacement placement = helper.placementAt(headIndex, eyeIndex);
        EyeAppearance look = helper.appearanceAt(headIndex, eyeIndex).overlay(overrides);

        // Eye offset in head-local coordinates, then the eye's own aim (not the head's) about its center.
        Vec3 position = placement.position();
        pose.translate(position.x, position.y, position.z);
        pose.mulPose(placement.orientation());

        // The pose now sits in this eye's pupil plane (post offset + aim, pre-scale): capture which way
        // world-down points here, for the next tick's gravity. This is the one spot that knows the eye's
        // fully-animated orientation.
        captureGravity(pose, eyeInfo);

        drawAnimatedEye(pose, bufferSource, packedLight, overlay, eyeInfo, placement.eyeScale(),
                placement.depth(), look.cornea(), look.iris(), placement.irisScale(), look.glow(), partialTicks);

        pose.popPose();
    }

    /**
     * Draw head {@code head}'s visible eyes at the current pose, which sits in that head's attachment space, each
     * simulated by its entry in {@code row}, or at rest when {@code row} is {@code null}. Returns whether any was
     * drawn.
     */
    public static boolean renderHead(PoseStack pose, MultiBufferSource bufferSource, int packedLight, int overlay,
                                     @Nullable GooglyTracker.EyeInfo[] row, HeadInfo helper,
                                     AppearanceOverride overrides, int head, float partialTicks) {
        boolean drew = false;
        int eyeCount = helper.getEyeCount(head);
        for (int eye = 0; eye < eyeCount; eye++) {
            if (helper.placementAt(head, eye).eyeScale() > 0F) {
                renderEye(pose, bufferSource, packedLight, overlay, row == null ? REST : row[eye], helper, overrides,
                        head, eye, partialTicks);
                drew = true;
            }
        }
        return drew;
    }

    /**
     * Draw a picker draft eye as a static preview (centered iris, no physics) at the current pose, which
     * the caller has already moved into the draft's attachment space. Used by both the vanilla and the
     * GeckoLib eye layers.
     */
    public static void renderPreviewEye(PoseStack pose, MultiBufferSource bufferSource,
                                        int packedLight, int overlay, EyeDraft eye) {
        pose.pushPose();
        pose.translate(eye.position[0], eye.position[1], eye.position[2]);
        pose.mulPose(EyePlacement.orientation(eye.inclination, eye.azimuth));
        float scale = eye.eyeScale;
        pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH * eye.depth);
        drawEye(pose, bufferSource, packedLight, overlay, eye.corneaColors, eye.irisColors, eye.irisScale,
                0F, 0F, eye.glows);
        pose.popPose();
    }

    /**
     * Draw the picker's selection gizmo and, when one is being shaped, the live draft eye, at the current pose,
     * which the caller has already moved into the active part's attachment space.
     */
    public static void renderPreviewPlacement(PoseStack pose, MultiBufferSource bufferSource, int packedLight,
                                              int overlay) {
        Gizmo.draw(pose, bufferSource);
        EyeDraft draft = PickerState.currentEye();
        if (draft != null) {
            renderPreviewEye(pose, bufferSource, packedLight, overlay, draft);
        }
    }

    private static GooglyTracker.EyeInfo rest() {
        GooglyTracker.EyeInfo rest = new GooglyTracker.EyeInfo();
        rest.deltaY = 0F;
        rest.prevDeltaY = 0F;
        return rest;
    }
}
