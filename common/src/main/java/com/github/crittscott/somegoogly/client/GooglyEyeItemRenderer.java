package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
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

    private static final HeldEye HELD = new HeldEye();

    private static ItemEyes.@Nullable FrameData frameContext;

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
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE * ModelGooglyEye.BASE_DEPTH);
        ModelGooglyEye.addExtents(pose.last().pose(), output);
    }

    private static boolean isFirstPersonHand(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    /** Animation for a head item in the current GUI, hand, or frame draw, if its state still matches. */
    @Nullable
    public static ItemEyes itemEyes(ItemStack stack) {
        ItemEyes gui = GuiItemEyes.current().eyes();
        if (gui != null && gui.matches(stack)) {
            return gui;
        }
        ItemEyes hand = HandItemEyes.current().eyes();
        if (hand != null && hand.matches(stack)) {
            return hand;
        }
        ItemEyes eye = frameContext == null ? null : frameContext.eye();
        return eye != null && eye.matches(stack) ? eye : null;
    }

    /** Partial tick supplied by the current GUI, hand, or frame presentation. */
    public static float itemPartialTick() {
        if (GuiItemEyes.current().eyes() != null) {
            return GuiItemEyes.current().partialTick();
        }
        if (HandItemEyes.current().eyes() != null) {
            return HandItemEyes.current().partialTick();
        }
        return frameContext == null ? 0F : frameContext.partialTick();
    }

    /** Drop held-eye state when leaving a world or server. */
    public static void reset() {
        frameContext = null;
        HELD.reset();
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
            pose.mulPose(Axis.XP.rotationDegrees(180));
        }
        // Items have no placement, so they keep the standard thickness (depth multiplier 1).
        ItemEyes gui = ctx == ItemDisplayContext.GUI ? GuiItemEyes.current().eyes() : null;
        ItemEyes hand = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? HandItemEyes.current().eyes() : null;
        ItemEyes.FrameData frame = ctx == ItemDisplayContext.FIXED ? frameContext : null;
        ItemEyes framed = frame == null ? null : frame.eye();
        GooglyTracker.EyeInfo animated = gui != null ? gui.eye
                : hand != null ? hand.eye : framed != null ? framed.eye : isFirstPersonHand(ctx) ? HELD.eye : null;
        if (gui != null || hand != null || framed != null) {
            GooglyEyeRenderer.captureItemGravity(pose, animated);
        }
        if (ctx == ItemDisplayContext.FIXED) {
            pose.scale(FRAME_DIAMETER_MULTIPLIER, FRAME_DIAMETER_MULTIPLIER, 1F);
        }
        if (animated != null) {
            float partialTick = gui != null ? GuiItemEyes.current().partialTick()
                    : hand != null ? HandItemEyes.current().partialTick()
                    : framed != null ? frame.partialTick()
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

    /** Advance the local player's held eye one tick, unless the game is paused; called from the client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isPaused()) {
            HELD.tick(minecraft.player, ClientEyeConfigs.behaviors());
        }
    }

    /** Scope animation to this item-state draw and restore any enclosing context even on failure. */
    public static void renderInFrame(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector buffer,
                                     int light, int overlay, int outlineColor, ItemEyes.@Nullable FrameData frame) {
        ItemEyes.FrameData previous = frameContext;
        frameContext = frame;
        try {
            item.submit(pose, buffer, light, overlay, outlineColor);
        } finally {
            frameContext = previous;
        }
    }
}
