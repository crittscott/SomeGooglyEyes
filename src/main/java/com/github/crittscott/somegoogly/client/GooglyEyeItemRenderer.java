package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.state.AppearanceOverride;
import com.github.crittscott.somegoogly.state.EyeColor;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.player.LocalPlayer;
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
 * ground it's static (pupil centred) for a clean icon.
 *
 * <p>This is the "item-model" {@code EyeHolder} render path foreshadowed in {@code EyeHolder}.
 *
 * <p>Tuning knobs if the eye sits wrong in the slot/hand: {@link #MODEL_SCALE} (size) and the
 * {@code XP.rotationDegrees(180)} (which faces the pupil at the viewer and lets it hang down).
 */
public class GooglyEyeItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEX =
            new ResourceLocation(SomeGoogly.MOD_ID, "textures/model/modelgooglyeye.png");

    /** Base model scale for hand/ground/item-frame (those contexts size further via the json display). */
    private static final float MODEL_SCALE = 0.22F;
    /**
     * Inventory size is set HERE, not in the model json: the GUI render path ignores a BEWLR's
     * {@code gui} display transform (the other contexts honour theirs), so this is the inventory knob.
     */
    private static final float GUI_SCALE = 1.8F;
    private static final float IRIS_SCALE = 0.6F;

    private ModelGooglyEye model;
    private final HeldWobble wobble = new HeldWobble();

    public GooglyEyeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private ModelGooglyEye model() {
        if (model == null) {
            model = new ModelGooglyEye(Minecraft.getInstance().getEntityModels().bakeLayer(SomeGoogly.GOOGLY_EYE_LAYER));
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
        // Face the pupil (model -Z) at the viewer and let it hang down; winding-safe (pure rotation).
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(scale, scale, scale * 0.4F);

        drawEye(m, pose, buffer.getBuffer(RenderType.entityCutout(TEX)), light, overlay, cornea, iris, irisX, irisY);
        if (glow) {
            drawEye(m, pose, buffer.getBuffer(RenderType.eyes(TEX)), light, overlay, cornea, iris, irisX, irisY);
        }
        pose.popPose();
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

    /**
     * Googly physics for a held eye, using the <b>same</b> {@link GooglyTracker.EyeInfo} per-tick step
     * as mob eyes (fed the holder's look + position deltas), so the behaviour is identical: it reacts
     * to movement and settles to rest when you stand still. Advanced with a fixed-timestep accumulator
     * off wall-clock time so it ticks ~20 Hz regardless of framerate.
     */
    private static final class HeldWobble {
        private final Random rand = new Random();
        private final GooglyTracker.EyeInfo eye = new GooglyTracker.EyeInfo();
        private boolean initialised;
        private long lastNanos;
        private double accumulatorTicks;
        private double prevX;
        private double prevY;
        private double prevZ;

        void update() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            long now = System.nanoTime();

            if (!initialised) {
                initialised = true;
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
}
