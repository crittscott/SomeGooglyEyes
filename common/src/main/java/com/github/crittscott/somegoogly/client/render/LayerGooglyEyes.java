package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;

/**
 * Eye layer for vanilla-style {@link LivingEntityRenderer}s. It draws from the {@link EyeRenderData}
 * it takes for the entity, which shares its visibility gate with the GeckoLib layer, resolves
 * configured attachment tokens through {@link Resolvers}, and delegates each eye's drawing to
 * {@link GooglyEyeRenderer}.
 *
 * <p>For the picker's target the layer draws the authoring preview instead: the edited variant's saved
 * eyes, the live draft eye, and the selection gizmo, with centered irises (no physics), since placement
 * is what matters there.
 *
 * <p>The GeckoLib counterpart is {@code GooglyGeoLayer}, which makes the same split from GeckoLib's
 * per-bone callback.
 */
public class LayerGooglyEyes<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    /** Attachment part whose eyes sit on a sheep's wool. */
    private static final String SHEEP_BODY_PART = "body";
    /** Thickness of the adult sheep's wool beyond its body, in blocks (1.75 model pixels). */
    private static final float SHEEP_WOOL_DEPTH = 1.75F / 16.0F;

    public LayerGooglyEyes(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T living,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        EyeRenderData data = EyeRenderData.extract(living, partialTicks);
        if (!data.pickerTarget() && data.helper() == null) {
            return;
        }

        // Resolve the part-tree strategy for this model family (string names / stable indices).
        M model = this.getParentModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        int overlay = LivingEntityRenderer.getOverlayCoords(living, 0.0F);

        if (data.pickerTarget()) {
            renderPreview(poseStack, bufferSource, packedLight, overlay, model, resolver);
        } else {
            renderEyes(poseStack, bufferSource, packedLight, overlay, living, model, resolver, data);
        }
    }

    private void renderEyes(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay,
                            T living, M model, EyeAttachmentResolver resolver, EyeRenderData data) {
        HeadInfo helper = data.helper();
        GooglyTracker tracker = data.tracker();
        // Per-mob appearance overrides (dye / redstone / harvested-eye item), layered on top of the
        // shared config below. Same AppearanceOverride an eye item carries; mirrored onto the tracker by
        // ClientNetworkHandler so this doesn't re-parse it from NBT every frame.
        AppearanceOverride overrides = tracker.overrides;
        float partialTicks = data.partialTick();

        int headCount = helper.getHeadCount();
        for (int headIndex = 0; headIndex < headCount; headIndex++) {
            poseStack.pushPose();
            // Move into this head's animated space, by the configured string part name.
            if (resolver.toAttachmentSpace(poseStack, model, helper.getAttachToken(headIndex))) {
                insetShearedSheepBody(poseStack, living, helper.getAttachToken(headIndex));
                GooglyEyeRenderer.renderHead(poseStack, bufferSource, packedLight, overlay,
                        tracker.eyes[headIndex], helper, overrides, headIndex, partialTicks);
            }
            poseStack.popPose();
        }
    }

    /**
     * Sheep body eyes are authored on the wool's surface, which the separate wool layer draws around the
     * body. A sheared adult shows no wool, so its body eyes move in along the body's length by the wool's
     * thickness to sit on the bare body. Lambs are never sheared.
     */
    private static void insetShearedSheepBody(PoseStack poseStack, LivingEntity living, String part) {
        if (living instanceof Sheep sheep && sheep.isSheared() && !sheep.isBaby()
                && SHEEP_BODY_PART.equals(part)) {
            poseStack.translate(0.0F, -SHEEP_WOOL_DEPTH, 0.0F);
        }
    }

    private void renderPreview(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay,
                               M model, EyeAttachmentResolver resolver) {
        for (PickerState.ListedEye listed : PickerState.previewEyes()) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, listed.part)) {
                GooglyEyeRenderer.renderPreviewEye(poseStack, bufferSource, packedLight, overlay, listed.eye);
            }
            poseStack.popPose();
        }

        // With no active part (the empty state) the mob shows just its saved eyes, or nothing at all.
        String token = PickerState.currentPart();
        if (token != null) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, token)) {
                GooglyEyeRenderer.renderPreviewPlacement(poseStack, bufferSource, packedLight, overlay);
            }
            poseStack.popPose();
        }
    }
}
