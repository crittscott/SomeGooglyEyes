package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.HeldEye;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

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
public class GooglyEyeItemRenderer implements SpecialModelRenderer<GooglyEyeItemRenderer.Frame> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "googly_eye");

    /** Mounted diameter multiplier; eye thickness is unchanged. */
    private static final float FRAME_DIAMETER_MULTIPLIER = 3F;
    private static final float GUI_SCALE = 1.8F;
    private static final float IRIS_SCALE = 0.6F;
    /** Base model scale for hand/ground/item-frame (those contexts size further via the json display). */
    private static final float MODEL_SCALE = 0.22F;

    /** Appearance and display context captured before deferred item submission. */
    public record Frame(AppearanceOverride appearance, ItemDisplayContext context) {
    }

    /** The unbaked form named by the item definition; it has no fields. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Frame> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Frame> bake(SpecialModelRenderer.BakingContext context) {
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
    public Frame extractArgument(ItemStack stack) {
        return extractArgument(stack, ItemDisplayContext.NONE);
    }

    /** Capture the display context with the appearance before deferred submission. */
    public Frame extractArgument(ItemStack stack, ItemDisplayContext context) {
        return new Frame(EyeItemProperties.get(stack), context);
    }

    /** Bounds of the inventory eye centered in the item box at GUI scale. */
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack pose = new PoseStack();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.XP, 180);
        pose.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE * ModelGooglyEye.BASE_DEPTH);
        ModelGooglyEye.addExtents(pose.last().pose(), output);
    }

    @Override
    public void submit(@Nullable Frame argument, PoseStack pose,
                       SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (argument == null) {
            return;
        }
        AppearanceOverride props = argument.appearance();
        ItemDisplayContext ctx = argument.context();
        EyeColor cornea = props.cornea().orElse(EyeColor.WHITE);
        EyeColor iris = props.iris().orElse(EyeColor.BLACK);
        boolean glow = props.glow().orElse(false);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // Face the pupil (model -Z) at the viewer; winding-safe (pure rotation). Hand/GUI/ground view the
        // eye from its +Z side, so flip 180° about X to bring the pupil — and iris — forward. An item
        // frame (FIXED) mounts the eye facing the other way, so that same flip buries the iris behind the
        // cornea; the unflipped model already aims the pupil out of the frame at the viewer.
        if (ctx != ItemDisplayContext.FIXED) {
            pose.rotateDegrees(Axis.XP, 180);
        }
        // Items have no placement, so they keep the standard thickness (depth multiplier 1).
        PresentationEyes.Draw draw = ClientEyeRuntime.itemDraw();
        PresentationEyes presented = draw.eyes();
        GooglyTracker.EyeInfo animated = presented != null ? presented.eye
                : isFirstPersonHand(ctx) ? ClientEyeRuntime.heldEye() : null;
        if (presented != null) {
            GooglyEyeRenderer.captureGravity(pose, animated);
        }
        if (ctx == ItemDisplayContext.FIXED) {
            pose.scale(FRAME_DIAMETER_MULTIPLIER, FRAME_DIAMETER_MULTIPLIER, 1F);
        }
        if (animated != null) {
            float partialTick = presented != null ? draw.partialTick()
                    : Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            GooglyEyeRenderer.drawAnimatedEye(pose, collector, light, overlay, animated,
                    ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE, 1F, cornea, iris,
                    IRIS_SCALE, glow, partialTick);
        } else {
            float scale = ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE;
            pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH);
            GooglyEyeRenderer.drawEye(pose, collector, light, overlay, cornea.toArray(), iris.toArray(), IRIS_SCALE,
                    0F, 0F, glow);
        }
        pose.popPose();
    }

}
