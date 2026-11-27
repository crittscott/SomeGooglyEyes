package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SheepRenderState;

/**
 * Eye layer for vanilla-style {@link LivingEntityRenderer}s. It draws from the {@link
 * EyeRenderData} taken at render-state extraction, which shares its visibility gate with the
 * GeckoLib layer, resolves configured attachment tokens through {@link Resolvers}, and delegates
 * each eye's drawing to {@link GooglyEyeRenderer}.
 *
 * <p>For the picker's target the layer draws the authoring preview instead: the edited variant's
 * saved eyes, the live draft eye, and the selection gizmo, with centered irises (no physics), since
 * placement is what matters there.
 *
 * <p>Vanilla poses its shared entity model only when the submitted model is drawn, after every
 * layer has submitted, so the layer poses the parent model for this render state itself before
 * walking its parts.
 *
 * <p>The GeckoLib counterpart is {@code GooglyGeoLayer}, which makes the same split from GeckoLib's
 * per-bone callback. {@code IceAndFireCompat} draws through {@link #submitEyes} from Ice and Fire
 * CE's own renderer features.
 */
public class LayerGooglyEyes<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {
    /** Thickness of the adult sheep's wool beyond its body, in blocks (1.75 model pixels). */
    private static final float SHEEP_WOOL_DEPTH = 1.75F / 16.0F;

    public LayerGooglyEyes(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    /** Move a body-attached eye in by {@code bodyInset}; see {@link #shearedBodyInset}. */
    private static void insetBody(PoseStack poseStack, float bodyInset, String part) {
        if (bodyInset != 0.0F && EyeConfigModel.PART_BODY.equals(part)) {
            poseStack.translate(0.0F, -bodyInset, 0.0F);
        }
    }

    private static void renderEyes(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int overlay,
            Object model,
            EyeAttachmentResolver resolver,
            EyeRenderData data,
            boolean young,
            float bodyInset) {
        HeadInfo helper = data.helper();
        GooglyTracker tracker = data.tracker();
        // Per-mob appearance overrides (dye / redstone / harvested-eye item), layered on top of the
        // shared config below. Same AppearanceOverride an eye item carries; mirrored onto the
        // tracker by ClientNetworkHandler so this doesn't re-parse it from NBT every frame.
        AppearanceOverride overrides = tracker.overrides;
        float partialTicks = data.partialTick();

        int headCount = helper.getHeadCount();
        for (int headIndex = 0; headIndex < headCount; headIndex++) {
            poseStack.pushPose();
            // Move into this head's animated space, by the configured string part name.
            if (resolver.toAttachmentSpace(
                    poseStack, model, helper.getAttachToken(headIndex), young)) {
                insetBody(poseStack, bodyInset, helper.getAttachToken(headIndex));
                GooglyEyeRenderer.renderHead(
                        poseStack,
                        collector,
                        packedLight,
                        overlay,
                        tracker.eyes[headIndex],
                        helper,
                        overrides,
                        headIndex,
                        partialTicks);
            }
            poseStack.popPose();
        }
    }

    private static void renderPreview(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int overlay,
            Object model,
            EyeAttachmentResolver resolver,
            boolean young,
            float bodyInset) {
        for (PickerState.ListedEye listed : PickerState.previewEyes()) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, listed.part, young)) {
                insetBody(poseStack, bodyInset, listed.part);
                GooglyEyeRenderer.renderPreviewEye(
                        poseStack, collector, packedLight, overlay, listed.eye);
            }
            poseStack.popPose();
        }

        // With no active part (the empty state) the mob shows just its saved eyes, or nothing at
        // all.
        String token = PickerState.currentPart();
        if (token != null) {
            poseStack.pushPose();
            if (resolver.toAttachmentSpace(poseStack, model, token, young)) {
                insetBody(poseStack, bodyInset, token);
                GooglyEyeRenderer.renderPreviewPlacement(
                        poseStack, collector, packedLight, overlay);
            }
            poseStack.popPose();
        }
    }

    /**
     * The adult sheep's wool thickness when {@code state} is a sheared adult sheep, else zero.
     * Sheep body eyes are authored on the wool's surface, which the separate wool layer draws
     * around the body; a sheared adult shows no wool, so its body eyes move in by the wool's
     * thickness to sit on the bare body. Lambs are never sheared.
     */
    private static float shearedBodyInset(LivingEntityRenderState state) {
        return state instanceof SheepRenderState sheep && sheep.isSheared && !sheep.isBaby
                ? SHEEP_WOOL_DEPTH
                : 0.0F;
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            S state,
            float yRot,
            float xRot) {
        EyeRenderData data = EyeRenderData.of(state);
        if (!data.draws()) {
            return;
        }
        M model = this.getParentModel();
        model.setupAnim(state);
        submitEyes(
                poseStack,
                collector,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                data,
                model,
                state.isBaby,
                shearedBodyInset(state));
    }

    /**
     * Draw {@code data}'s eyes on {@code model}, or the picker preview when the entity is the
     * picker's target. {@code poseStack} holds the model's base render pose and the model's parts
     * are already posed for this entity. Renderers outside the vanilla layer system call this too.
     *
     * @param young the entity's baby flag, which selects the model's age branch
     * @param bodyInset how far body-attached eyes move in from where they were authored
     */
    public static void submitEyes(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int overlay,
            EyeRenderData data,
            Object model,
            boolean young,
            float bodyInset) {
        // Resolve the part-tree strategy for this model family (string names / stable indices).
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (data.pickerTarget()) {
            renderPreview(
                    poseStack, collector, packedLight, overlay, model, resolver, young, bodyInset);
        } else {
            renderEyes(
                    poseStack,
                    collector,
                    packedLight,
                    overlay,
                    model,
                    resolver,
                    data,
                    young,
                    bodyInset);
        }
    }
}
