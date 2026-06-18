package com.github.crittscott.somegoogly.render;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.state.EyeProperties;
import com.github.crittscott.somegoogly.state.EyeState;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class LayerGooglyEyes<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEX_GOOGLY_EYE = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEX_GOOGLY_EYE);
    private static final RenderType RENDER_TYPE_EYES = RenderType.eyes(TEX_GOOGLY_EYE);

    // Keystone B expression tuning.
    private static final float HURT_GROW = 0.5F;     // extra eye scale at peak hurt
    private static final float BLINK_SQUASH = 0.95F; // vertical squash at full blink (→ ~5% height)
    private static final float SWIRL_RADIUS = 0.8F;  // pupil orbit radius during a villager swirl
    private static final float[] ANGER_COLOR = {1.0F, 0.0F, 0.0F};

    private final ModelGooglyEye modelGooglyEye;

    public LayerGooglyEyes(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye(Minecraft.getInstance().getEntityModels().bakeLayer(SomeGoogly.GOOGLY_EYE_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T living, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        // While the picker is editing this entity, only its preview layer should draw.
        if (PickerState.isActiveTarget(living)) {
            return;
        }

        // Client global disable check
        if (ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            return;
        }

        // Client entity-specific disable check
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (ClientConfig.isEntityDisabled(entityType)) {
            return;
        }

        // Check the server's spawn decision (NBT). While the picker is active we bypass this so every
        // eye-configured mob shows eyes for authoring, without having to raise the spawn-chance config.
        if (!PickerState.active && !EyeState.hasEyes(living)) {
            return;
        }

        HeadInfo helper = HeadInfo.getHelper(entityType, living);
        if (helper == null || !helper.hasConfig()) {
            return;
        }

        // Per-mob appearance overrides (dye / redstone / harvested-eye item), layered on top of the
        // shared config below. Same EyeProperties an eye item carries.
        EyeProperties overrides = EyeState.readProperties(living);

        // Resolve the part-tree strategy for this model family (string names / stable indices).
        M model = this.getParentModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (resolver == null) {
            return;
        }

        GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        tracker.setLastUpdateRequest();
        tracker.requireUpdate();

        // Keystone B: interpolate the per-mob expression scalars once for this frame.
        boolean blinkEnabled = ClientConfig.EXPRESSION_BLINK.get();
        float hurtLerp = ClientConfig.EXPRESSION_HURT_GROW.get() ? lerp(tracker.prevHurt, tracker.hurt, partialTicks) : 0F;
        float angerLerp = ClientConfig.EXPRESSION_ANGER.get() ? lerp(tracker.prevAnger, tracker.anger, partialTicks) : 0F;
        float stareLerp = ClientConfig.EXPRESSION_STARE.get() ? lerp(tracker.prevStareAmount, tracker.stareAmount, partialTicks) : 0F;
        boolean swirling = ClientConfig.EXPRESSION_SWIRL.get() && tracker.isSwirling();
        float swirlAngleLerp = lerp(tracker.prevSwirlAngle, tracker.swirlAngle, partialTicks);
        float grow = 1F + hurtLerp * HURT_GROW;

        int headCount = helper.getHeadCount();
        for (int headIndex = 0; headIndex < headCount; headIndex++) {
            poseStack.pushPose();

            // Move into this head's animated space, by the configured string part name.
            if (!resolver.toAttachmentSpace(poseStack, model, helper.getAttachToken(headIndex))) {
                poseStack.popPose();
                continue;
            }

            int eyeCount = helper.getEyeCount(headIndex);
            for (int eyeIndex = 0; eyeIndex < eyeCount; eyeIndex++) {
                if (living.isInvisible() && helper.affectedByInvisibility(headIndex, eyeIndex)) {
                    continue;
                }

                float eyeScale = helper.getEyeScale(headIndex, eyeIndex);
                if (eyeScale <= 0F) {
                    continue;
                }

                poseStack.pushPose();

                // Apply eye offset in head-local coordinates
                float[] eyes = helper.getEyeOffsetFromJoint(headIndex, eyeIndex);
                poseStack.translate(eyes[0] + helper.getEyeSideOffset(headIndex, eyeIndex), eyes[1], eyes[2]);

                // Apply the eye's orientation (its own aim, not the head's) about the eye centre.
                HeadInfo.applyRotation(poseStack, helper.getInclination(headIndex, eyeIndex), helper.getAzimuth(headIndex, eyeIndex));

                GooglyTracker.EyeInfo eyeInfo = tracker.eyes[headIndex][eyeIndex];

                // Keystone B: hurt-grow scales the whole eye; blink squashes it vertically.
                float blinkLerp = blinkEnabled ? lerp(eyeInfo.prevBlink, eyeInfo.blink, partialTicks) : 0F;
                float blinkY = 1F - blinkLerp * BLINK_SQUASH;
                poseStack.scale(eyeScale * grow, eyeScale * grow * blinkY, eyeScale * 0.4F * grow);

                VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);
                int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

                float[] corneaColours = overrides.corneaColor().isPresent()
                        ? EyeState.unpackColor(overrides.corneaColor().getAsInt())
                        : helper.getCorneaColours(headIndex, eyeIndex);
                // Keystone B: tint the eyeball toward red while angry.
                if (angerLerp > 0F) {
                    corneaColours = lerpColor(corneaColours, ANGER_COLOR, angerLerp);
                }
                modelGooglyEye.renderCornea(poseStack, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

                float[] irisColours = overrides.irisColor().isPresent()
                        ? EyeState.unpackColor(overrides.irisColor().getAsInt())
                        : helper.getIrisColours(headIndex, eyeIndex);
                float irisScale = helper.getIrisScale(headIndex, eyeIndex);

                poseStack.pushPose();
                poseStack.scale(irisScale, irisScale, 1F);

                // Iris position: physics wobble, pulled toward centre while staring, or driven in a
                // circle during a villager level-up swirl.
                float irisX;
                float irisY;
                if (swirling) {
                    irisX = (float) Math.cos(swirlAngleLerp) * SWIRL_RADIUS;
                    irisY = (float) Math.sin(swirlAngleLerp) * SWIRL_RADIUS;
                } else {
                    irisX = lerp(eyeInfo.prevDeltaX, eyeInfo.deltaX, partialTicks) * (1F - stareLerp);
                    irisY = lerp(eyeInfo.prevDeltaY, eyeInfo.deltaY, partialTicks) * (1F - stareLerp);
                }
                modelGooglyEye.moveIris(irisX, irisY, irisScale);

                modelGooglyEye.renderIris(poseStack, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
                poseStack.popPose();

                boolean glow = overrides.glow().isPresent()
                        ? overrides.glow().get()
                        : helper.doesEyeGlow(headIndex, eyeIndex);
                if (glow) {
                    buffer = bufferSource.getBuffer(RENDER_TYPE_EYES);
                    modelGooglyEye.renderCornea(poseStack, buffer, packedLight, overlay, corneaColours[0], corneaColours[1], corneaColours[2], 1F);

                    poseStack.pushPose();
                    poseStack.scale(irisScale, irisScale, 1F);
                    modelGooglyEye.renderIris(poseStack, buffer, packedLight, overlay, irisColours[0], irisColours[1], irisColours[2], 1F);
                    poseStack.popPose();
                }

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float[] lerpColor(float[] from, float[] to, float t) {
        return new float[]{lerp(from[0], to[0], t), lerp(from[1], to[1], t), lerp(from[2], to[2], t)};
    }
}
