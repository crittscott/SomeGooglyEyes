package com.github.crittscott.somegoogly.render;

import com.github.crittscott.somegoogly.config.ClientConfig;
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
 * potion / NBT) and the Keystone&nbsp;B expression logic (blink, hurt-grow, anger tint, stare,
 * swirl) here means the two layers can't drift apart: a mob looks the same whether its model is
 * vanilla or GeckoLib.
 *
 * <p>The caller is responsible only for moving the pose into the head's animated attachment space;
 * this class handles everything from the eye's own offset/rotation inward.
 */
public final class GooglyEyeRenderer {

    private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    public static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX);
    public static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX);

    // Keystone B expression tuning (shared so both layers match).
    private static final float HURT_GROW = 0.5F;     // extra eye scale at peak hurt
    private static final float BLINK_SQUASH = 0.95F; // vertical squash at full blink (→ ~5% height)
    private static final float SWIRL_RADIUS = 0.8F;  // pupil orbit radius during a villager swirl
    private static final float[] ANGER_COLOR = {1.0F, 0.0F, 0.0F};

    private GooglyEyeRenderer() {
    }

    /**
     * Per-frame expression scalars, interpolated once and reused for every eye on the mob. Honours
     * the client {@code EXPRESSION_*} toggles, so a disabled expression contributes nothing.
     */
    public static final class Frame {
        final boolean blinkEnabled;
        final float grow;
        final float angerLerp;
        final float stareLerp;
        final boolean swirling;
        final float swirlAngleLerp;

        private Frame(boolean blinkEnabled, float grow, float angerLerp, float stareLerp,
                      boolean swirling, float swirlAngleLerp) {
            this.blinkEnabled = blinkEnabled;
            this.grow = grow;
            this.angerLerp = angerLerp;
            this.stareLerp = stareLerp;
            this.swirling = swirling;
            this.swirlAngleLerp = swirlAngleLerp;
        }

        public static Frame capture(GooglyTracker tracker, float partialTicks) {
            boolean blinkEnabled = ClientConfig.EXPRESSION_BLINK.get();
            float hurtLerp = ClientConfig.EXPRESSION_HURT_GROW.get() ? lerp(tracker.prevHurt, tracker.hurt, partialTicks) : 0F;
            float angerLerp = ClientConfig.EXPRESSION_ANGER.get() ? lerp(tracker.prevAnger, tracker.anger, partialTicks) : 0F;
            float stareLerp = ClientConfig.EXPRESSION_STARE.get() ? lerp(tracker.prevStareAmount, tracker.stareAmount, partialTicks) : 0F;
            boolean swirling = ClientConfig.EXPRESSION_SWIRL.get() && tracker.isSwirling();
            float swirlAngleLerp = lerp(tracker.prevSwirlAngle, tracker.swirlAngle, partialTicks);
            float grow = 1F + hurtLerp * HURT_GROW;
            return new Frame(blinkEnabled, grow, angerLerp, stareLerp, swirling, swirlAngleLerp);
        }
    }

    /**
     * Draw a single eye, applying both the per-mob {@code overrides} and the per-frame expression
     * {@code frame}. The pose is expected to already sit in the head's attachment space; this method
     * applies the eye's own offset/rotation/scale within its own push/pop, so it leaves the pose
     * stack as it found it.
     */
    public static void renderEye(PoseStack pose, ModelGooglyEye model, MultiBufferSource bufferSource,
                                 int packedLight, int overlay, Frame frame, GooglyTracker tracker,
                                 HeadInfo helper, EyeProperties overrides, int headIndex, int eyeIndex,
                                 float partialTicks) {
        pose.pushPose();

        // Eye offset in head-local coordinates, then the eye's own aim (not the head's) about its centre.
        float[] eyes = helper.getEyeOffsetFromJoint(headIndex, eyeIndex);
        pose.translate(eyes[0] + helper.getEyeSideOffset(headIndex, eyeIndex), eyes[1], eyes[2]);
        HeadInfo.applyRotation(pose, helper.getInclination(headIndex, eyeIndex), helper.getAzimuth(headIndex, eyeIndex));

        GooglyTracker.EyeInfo eyeInfo = tracker.eyes[headIndex][eyeIndex];

        // Hurt-grow scales the whole eye; blink squashes it vertically.
        float blinkLerp = frame.blinkEnabled ? lerp(eyeInfo.prevBlink, eyeInfo.blink, partialTicks) : 0F;
        float blinkY = 1F - blinkLerp * BLINK_SQUASH;
        float eyeScale = helper.getEyeScale(headIndex, eyeIndex);
        pose.scale(eyeScale * frame.grow, eyeScale * frame.grow * blinkY, eyeScale * 0.4F * frame.grow);

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);

        float[] corneaColours = overrides.corneaColor().isPresent()
                ? EyeState.unpackColor(overrides.corneaColor().getAsInt())
                : helper.getCorneaColours(headIndex, eyeIndex);
        // Tint the eyeball toward red while angry.
        if (frame.angerLerp > 0F) {
            corneaColours = lerpColor(corneaColours, ANGER_COLOR, frame.angerLerp);
        }
        model.renderCornea(pose, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

        float[] irisColours = overrides.irisColor().isPresent()
                ? EyeState.unpackColor(overrides.irisColor().getAsInt())
                : helper.getIrisColours(headIndex, eyeIndex);
        float irisScale = helper.getIrisScale(headIndex, eyeIndex);

        pose.pushPose();
        pose.scale(irisScale, irisScale, 1F);

        // Iris position: physics wobble, pulled toward centre while staring, or driven in a circle
        // during a villager level-up swirl.
        float irisX;
        float irisY;
        if (frame.swirling) {
            irisX = (float) Math.cos(frame.swirlAngleLerp) * SWIRL_RADIUS;
            irisY = (float) Math.sin(frame.swirlAngleLerp) * SWIRL_RADIUS;
        } else {
            irisX = lerp(eyeInfo.prevDeltaX, eyeInfo.deltaX, partialTicks) * (1F - frame.stareLerp);
            irisY = lerp(eyeInfo.prevDeltaY, eyeInfo.deltaY, partialTicks) * (1F - frame.stareLerp);
        }
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
