package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.HeldEye;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Renders a {@code googly_eye} item as the actual 3D {@link ModelGooglyEye}, tinted by the item's
 * {@link AppearanceOverride}. Reuses the in-world eye model so the item and the mob eyes can't drift.
 *
 * <p>When the local player holds the item, its first-person view is a live eye, the {@link HeldEye} stepped
 * from the client tick: its pupil moves with the player's look and movement plus gravity, and it plays its
 * own behaviors. Framed eyes have independent mounted pupil physics and ambient expressions, supplied
 * by the frame's extracted render state. GUI and third-person hand presentations have independent ambient animation; ground views are static.
 *
 * <p>Registered as the {@code somegoogly:googly_eye} special model type and selected by
 * {@code assets/somegoogly/items/googly_eye.json}; each loader puts {@link Unbaked#MAP_CODEC} under
 * {@link #ID}.
 *
 * <p>Draws through {@link GooglyEyeRenderer#drawEye}, so the item and the mob eyes share one drawing
 * path, texture, and pair of render types.
 */
public class GooglyEyeItemRenderer implements SpecialModelRenderer<AppearanceOverride> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "googly_eye");

    /** Mounted diameter multiplier; eye thickness is unchanged. */
    private static final float FRAME_DIAMETER_MULTIPLIER = 3F;
    private static final float GUI_SCALE = 1.8F;
    private static final float IRIS_SCALE = 0.6F;
    /** Base model scale for hand/ground/item-frame (those contexts size further via the json display). */
    private static final float MODEL_SCALE = 0.22F;

    /** The unbaked form named by the item definition; it has no fields. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new GooglyEyeItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }

    private static boolean isFirstPersonHand(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    @Override
    public AppearanceOverride extractArgument(ItemStack stack) {
        return EyeItemProperties.get(stack);
    }

    @Override
    public void render(@Nullable AppearanceOverride argument, ItemDisplayContext ctx, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay, boolean hasFoil) {
        EyeAppearance look = EyeAppearance.DEFAULT.overlay(argument != null ? argument : AppearanceOverride.EMPTY);
        PresentationEyes.Draw draw = ClientEyeRuntime.itemDraw();
        PresentationEyes presented = draw.eyes();

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
                : isFirstPersonHand(ctx) ? ClientEyeRuntime.heldEye() : null;
        if (presented != null) {
            GooglyEyeRenderer.captureGravity(pose, animated);
        }
        if (ctx == ItemDisplayContext.FIXED) {
            pose.scale(FRAME_DIAMETER_MULTIPLIER, FRAME_DIAMETER_MULTIPLIER, 1F);
        }
        if (animated != null) {
            // First-person eyes retain eye-local down; their pose is relative to the camera rather than the world.
            float partialTick = presented != null ? draw.partialTick()
                    : Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            GooglyEyeRenderer.drawAnimatedEye(pose, buffer, light, overlay, animated,
                    ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE, 1F, look.cornea(), look.iris(),
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
