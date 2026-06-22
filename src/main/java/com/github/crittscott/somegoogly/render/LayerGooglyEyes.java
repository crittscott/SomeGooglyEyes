package com.github.crittscott.somegoogly.render;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.state.AppearanceOverride;
import com.github.crittscott.somegoogly.state.EyeState;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class LayerGooglyEyes<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final ModelGooglyEye modelGooglyEye;

    public LayerGooglyEyes(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.modelGooglyEye = new ModelGooglyEye();
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
        // shared config below. Same AppearanceOverride an eye item carries.
        AppearanceOverride overrides = EyeState.readProperties(living);

        // Resolve the part-tree strategy for this model family (string names / stable indices).
        M model = this.getParentModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (resolver == null) {
            return;
        }

        GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
        tracker.setLastUpdateRequest();
        tracker.requireUpdate();

        int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

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

                if (helper.getEyeScale(headIndex, eyeIndex) <= 0F) {
                    continue;
                }

                GooglyEyeRenderer.renderEye(poseStack, modelGooglyEye, bufferSource, packedLight, overlay,
                        tracker, helper, overrides, headIndex, eyeIndex, partialTicks);
            }

            poseStack.popPose();
        }
    }
}
