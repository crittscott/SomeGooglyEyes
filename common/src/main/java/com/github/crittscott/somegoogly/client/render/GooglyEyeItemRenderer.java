package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.eye.EyeAppearance;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;

/**
 * Renders a {@code googly_eye} item as the actual 3D {@link ModelGooglyEye}, tinted by the item's
 * {@link AppearanceOverride}. Reuses the in-world eye model so the item and the mob eyes can't drift.
 *
 * <p>Both hand views select one holder/hand presentation. The server supplies hurt/heal starts to the holder
 * and other viewers; GUI presentations have their own clock, and ground views are static.
 *
 * <p>Each loader attaches it as the Googly Eye's custom item renderer; {@code models/item/googly_eye.json}
 * selects the builtin entity model that hands rendering to it.
 *
 * <p>Draws through {@link GooglyEyeRenderer#drawEye}, so the item and the mob eyes share one drawing
 * path, texture, and pair of render types.
 */
public class GooglyEyeItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Eye-local down for the camera-relative first-person pose. */
    private static final Matrix3f EYE_LOCAL_POSE = new Matrix3f().scaling(-1F, -1F, 1F);
    /** Enlarge the mounted eye's diameter without changing its thickness. */
    private static final float FRAME_DIAMETER_MULTIPLIER = 3F;
    /**
     * Inventory size is set HERE, not in the model json: the GUI render path ignores a BEWLR's
     * {@code gui} display transform (the other contexts honor theirs), so this is the inventory knob.
     */
    private static final float GUI_SCALE = 1.8F;
    /** Fixed item appearance, independent of the mob placement defaults. */
    private static final float IRIS_SCALE = 0.6F;
    /** Base model scale for hand/ground/item-frame (those contexts size further via the json display). */
    private static final float MODEL_SCALE = 0.22F;

    public GooglyEyeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static boolean isFirstPersonHand(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        PresentationEyes.Draw draw = ClientEyeRuntime.itemDraw(stack);
        PresentationEyes presented = draw.eyes();
        EyeAppearance look = EyeAppearance.DEFAULT.overlay(
                presented != null ? presented.properties : EyeItemProperties.get(stack));

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // Face the pupil (model -Z) at the viewer; winding-safe (pure rotation). Hand/GUI/ground view the
        // eye from its +Z side, so flip 180° about X to bring the pupil — and iris — forward. An item
        // frame (FIXED) mounts the eye facing the other way, so that same flip buries the iris behind the
        // cornea; the unflipped model already aims the pupil out of the frame at the viewer.
        if (ctx != ItemDisplayContext.FIXED) {
            pose.mulPose(Axis.XP.rotationDegrees(180));
        }
        // Items have no placement, so they keep the standard thickness (depth multiplier 1).
        GooglyTracker.EyeInfo animated = presented != null ? presented.eye
                : null;
        if (animated != null && isFirstPersonHand(ctx)) {
            animated.itemGravity = false;
            animated.capturePose(EYE_LOCAL_POSE, null);
        }
        if (presented != null && !isFirstPersonHand(ctx)) {
            animated.itemGravity = ctx != ItemDisplayContext.GUI;
            GooglyEyeRenderer.captureGravity(pose, animated);
        }
        if (ctx == ItemDisplayContext.FIXED) {
            pose.scale(FRAME_DIAMETER_MULTIPLIER, FRAME_DIAMETER_MULTIPLIER, 1F);
        }
        if (animated != null) {
            // First-person eyes retain eye-local down; their pose is relative to the camera rather than the world.
            float partialTick = draw.partialTick();
            GooglyEyeRenderer.drawAnimatedEye(pose, buffer, light, overlay, animated,
                    ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE, EyePlacement.DEFAULT_DEPTH, look.cornea(), look.iris(),
                    IRIS_SCALE, look.glow(), partialTick);
        } else {
            float scale = ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE;
            pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH);
            GooglyEyeRenderer.drawEye(pose, buffer, light, overlay, look.cornea().toArray(), look.iris().toArray(),
                    IRIS_SCALE, 0F, 0F, look.glow());
        }
        pose.popPose();
    }
}
