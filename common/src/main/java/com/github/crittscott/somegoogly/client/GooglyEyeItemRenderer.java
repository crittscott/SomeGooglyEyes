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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Renders a {@code googly_eye} item as the actual 3D {@link ModelGooglyEye}, tinted by the item's
 * {@link AppearanceOverride}. Reuses the in-world eye model so the item and the mob eyes can't drift.
 *
 * <p>When the local player holds the item, its first-person view is a live eye, the {@link HeldEye} stepped
 * from the client tick: its pupil moves with the player's look and movement plus gravity, and it plays its
 * own behaviors. Every other view (inventory, item frame, ground, and any player's hand in third person, since a special model
 * renderer is not told who holds the item) is static, pupil centered.
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

    /**
     * Inventory size is set HERE, not in the model json: the inventory needs about eight times the
     * base scale, and vanilla caps a display transform's scale at 4.
     */
    private static final float GUI_SCALE = 1.8F;
    private static final float IRIS_SCALE = 0.6F;
    /** Base model scale for hand/ground/item-frame (those contexts size further via the json display). */
    private static final float MODEL_SCALE = 0.22F;

    private static final HeldEye HELD = new HeldEye();

    /** The unbaked form named by the item definition; it has no fields. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new GooglyEyeItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
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

    @Override
    public AppearanceOverride extractArgument(ItemStack stack) {
        return EyeItemProperties.get(stack);
    }

    /** The inventory view, the largest of the static views: the eye centered in the item box at GUI scale. */
    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack pose = new PoseStack();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE * ModelGooglyEye.BASE_DEPTH);
        ModelGooglyEye.addExtents(pose.last().pose(), output);
    }

    @Override
    public void submit(@Nullable AppearanceOverride argument, ItemDisplayContext ctx, PoseStack pose,
                       SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        AppearanceOverride props = argument != null ? argument : AppearanceOverride.EMPTY;
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
        GooglyTracker.EyeInfo held = isFirstPersonHand(ctx) ? HELD.eye : null;
        if (held != null) {
            // The live eye keeps eye-local down for gravity: this pose is relative to the camera, not the world.
            GooglyEyeRenderer.drawAnimatedEye(pose, collector, light, overlay, held, MODEL_SCALE, 1F, cornea, iris,
                    IRIS_SCALE, glow, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        } else {
            float scale = ctx == ItemDisplayContext.GUI ? GUI_SCALE : MODEL_SCALE;
            pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH);
            GooglyEyeRenderer.drawEye(pose, collector, light, overlay, cornea.toArray(), iris.toArray(), IRIS_SCALE,
                    0F, 0F, glow);
        }
        pose.popPose();
    }
}
