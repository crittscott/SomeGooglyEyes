package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a {@code googly_eye} item as the actual 3D {@link ModelGooglyEye}, tinted by the item's
 * {@link AppearanceOverride}. Reuses the in-world eye model so the item and the mob eyes can't drift.
 *
 * <p>When the local player holds the item, its first-person view is a live eye, the {@link HeldEye} stepped
 * from the client tick: its pupil moves with the player's look and movement plus gravity, and it plays its
 * own behaviors. Every other view (inventory, item frame, ground, and any player's hand in third person,
 * since an item renderer is not told who holds the item) is static, pupil centered.
 *
 * <p>Each loader attaches it as the Googly Eye's custom item renderer; {@code models/item/googly_eye.json}
 * selects the builtin entity model that hands rendering to it.
 *
 * <p>Draws through {@link GooglyEyeRenderer#drawEye}, so the item and the mob eyes share one drawing
 * path, texture, and pair of render types.
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

    private static final HeldEye HELD = new HeldEye();

    private ModelGooglyEye model;

    public GooglyEyeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Advance the local player's held eye one tick, unless the game is paused; called from the client tick. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isPaused()) {
            HELD.tick(minecraft.player, ClientEyeConfigs.behaviors());
        }
    }

    /** Drop held-eye state when leaving a world or server. */
    public static void reset() {
        HELD.reset();
    }

    private static boolean isFirstPersonHand(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
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
        AppearanceOverride props = EyeItemProperties.get(stack);
        EyeColor cornea = props.cornea().orElse(EyeColor.WHITE);
        EyeColor iris = props.iris().orElse(EyeColor.BLACK);
        boolean glow = props.glow().orElse(false);

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
        // Items have no placement, so they keep the standard thickness (depth multiplier 1).
        GooglyTracker.EyeInfo held = isFirstPersonHand(ctx) ? HELD.eye : null;
        if (held != null) {
            // The live eye keeps eye-local down for gravity: this pose is relative to the camera, not the world.
            GooglyEyeRenderer.drawAnimatedEye(pose, m, buffer, light, overlay, held, MODEL_SCALE, 1F, cornea, iris,
                    IRIS_SCALE, glow, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        } else {
            float scale = ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE;
            pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH);
            GooglyEyeRenderer.drawEye(pose, m, buffer, light, overlay, cornea.toArray(), iris.toArray(), IRIS_SCALE,
                    0F, 0F, glow);
        }
        pose.popPose();
    }
}
