package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * Renders a {@code googly_eye} item as the actual 3D {@link ModelGooglyEye}, tinted by the item's
 * {@link AppearanceOverride}. Reuses the in-world eye model so the item and the mob eyes can't drift.
 *
 * <p>When the item is <b>held</b> (any hand context) the pupil is alive — a small standalone googly
 * physics driven by the holder's look movement plus gravity. In the inventory / item frame / on the
 * ground it's static (pupil centered) for a clean icon.
 *
 * <p>This is the "item-model" {@code EyeHolder} render path foreshadowed in {@code EyeHolder}.
 *
 * <p>Tuning knobs if the eye sits wrong in the slot/hand: {@link #MODEL_SCALE} (size) and the
 * {@code XP.rotationDegrees(180)} (which faces the pupil at the viewer and lets it hang down).
 */
public class GooglyEyeItemRenderer extends BlockEntityWithoutLevelRenderer {

    /**
     * Inventory size is set HERE, not in the model json: the GUI render path ignores a BEWLR's
     * {@code gui} display transform (the other contexts honor theirs), so this is the inventory knob.
     */
    private static final float GUI_SCALE = 1.8F;
    private static final float IRIS_SCALE = 0.6F;
    /** Base model scale for hand/ground/item-frame (those contexts size further via the json display). */
    private static final float MODEL_SCALE = 0.22F;
    private static final ResourceLocation TEX =
            new ResourceLocation(SomeGoogly.MOD_ID, "textures/model/modelgooglyeye.png");

    private ModelGooglyEye model;
    private final HeldWobble wobble = new HeldWobble();

    public GooglyEyeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /**
     * Googly physics for a held eye, using the <b>same</b> {@link GooglyTracker.EyeInfo} per-tick step
     * as mob eyes (fed the holder's look + position deltas), so the behavior is identical: it reacts
     * to movement and settles to rest when you stand still. Advanced with a fixed-timestep accumulator
     * off wall-clock time so it ticks ~20 Hz regardless of framerate.
     */
    private static final class HeldWobble {
        private double accumulatorTicks;
        private final GooglyTracker.EyeInfo eye = new GooglyTracker.EyeInfo();
        private boolean initialized;
        private long lastNanos;
        private double prevX;
        private double prevY;
        private double prevZ;
        private final Random rand = new Random();

        void update() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            long now = System.nanoTime();

            if (!initialized) {
                initialized = true;
                lastNanos = now;
                prevX = player.getX();
                prevY = player.getY();
                prevZ = player.getZ();
                // Prime prev-rotation so the first real step doesn't see a spike from zero.
                eye.update(rand, player.getYHeadRot(), player.getXRot(), 0, 0, 0);
                return;
            }

            accumulatorTicks += Math.min((now - lastNanos) / 5.0e7, 4.0); // 1 tick = 50 ms; cap big gaps
            lastNanos = now;

            boolean motionApplied = false;
            while (accumulatorTicks >= 1.0) {
                accumulatorTicks -= 1.0;
                double mx = 0;
                double my = 0;
                double mz = 0;
                if (!motionApplied) {
                    // Apply the accumulated position delta on the first step of this batch; update the
                    // anchor only here so motion isn't lost on frames that don't advance a tick.
                    mx = player.getX() - prevX;
                    my = player.getY() - prevY;
                    mz = player.getZ() - prevZ;
                    prevX = player.getX();
                    prevY = player.getY();
                    prevZ = player.getZ();
                    motionApplied = true;
                }
                eye.update(rand, player.getYHeadRot(), player.getXRot(), mx, my, mz);
            }
        }

        float x() {
            return eye.deltaX;
        }

        float y() {
            return eye.deltaY;
        }
    }

    private static void drawEye(ModelGooglyEye m, PoseStack pose, VertexConsumer vc, int light, int overlay,
                                float[] cornea, float[] iris, float irisX, float irisY) {
        m.renderCornea(pose, vc, light, overlay, cornea[0], cornea[1], cornea[2], 1F);
        pose.pushPose();
        pose.scale(IRIS_SCALE, IRIS_SCALE, 1F);
        m.moveIris(irisX, irisY, IRIS_SCALE);
        m.renderIris(pose, vc, light, overlay, iris[0], iris[1], iris[2], 1F);
        pose.popPose();
    }

    private static boolean isHeld(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private ModelGooglyEye model() {
        if (model == null) {
            model = new ModelGooglyEye();
        }
        return model;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        AppearanceOverride props = GooglyEyeItem.getProperties(stack);
        float[] cornea = props.cornea().orElse(EyeColor.WHITE).toArray();
        float[] iris = props.iris().orElse(EyeColor.BLACK).toArray();
        boolean glow = props.glow().orElse(false);

        float irisX = 0F;
        float irisY = 0F;
        if (isHeld(ctx)) {
            wobble.update();
            irisX = wobble.x();
            irisY = wobble.y();
        }

        float scale = ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE;

        ModelGooglyEye m = model();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // Face the pupil (model -Z) at the viewer; winding-safe (pure rotation). Hand/GUI/ground view the
        // eye from its +Z side, so flip 180° about X to bring the pupil — and iris — forward. An item
        // frame (FIXED) mounts the eye facing the other way, so that same flip buries the iris behind the
        // cornea; the unflipped model already aims the pupil out of the frame at the viewer.
        if (ctx != ItemDisplayContext.FIXED) {
            pose.mulPose(Axis.XP.rotationDegrees(180));
        }
        pose.scale(scale, scale, scale * 0.4F);

        drawEye(m, pose, buffer.getBuffer(RenderType.entityCutout(TEX)), light, overlay, cornea, iris, irisX, irisY);
        if (glow) {
            drawEye(m, pose, buffer.getBuffer(RenderType.eyes(TEX)), light, overlay, cornea, iris, irisX, irisY);
        }
        pose.popPose();
    }
}
