package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.render.GooglyEyeRenderer;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Renders a {@code googly_eye} item as the actual 3D {@link ModelGooglyEye}, tinted by the item's
 * {@link AppearanceOverride}. Reuses the in-world eye model so the item and the mob eyes can't drift.
 *
 * <p>When the local player holds the item, its first-person view has a live pupil — a small standalone
 * googly physics driven by the player's look movement plus gravity, stepped from the client tick. Every
 * other view (inventory, item frame, ground, and any player's hand in third person, since a special model
 * renderer is not told who holds the item) is static, pupil centered.
 *
 * <p>Registered as the {@code somegoogly:googly_eye} special model type and selected by
 * {@code assets/somegoogly/items/googly_eye.json}; each loader puts {@link Unbaked#MAP_CODEC} under
 * {@link #ID}.
 *
 * <p>Tuning knobs if the eye sits wrong in the slot/hand: {@link #GUI_SCALE} and {@link #MODEL_SCALE}
 * (size) and the {@code XP.rotationDegrees(180)} (which faces the pupil at the viewer and lets it hang
 * down).
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

    private static final HeldWobble WOBBLE = new HeldWobble();

    private ModelGooglyEye model;

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

    /** Advance the held-eye pupil one tick; called from the client tick. */
    public static void tick() {
        WOBBLE.tick();
    }

    /** Drop held-eye state when leaving a world or server. */
    public static void reset() {
        WOBBLE.reset();
    }

    /**
     * Googly physics for the local player's held eye, using the <b>same</b> {@link GooglyTracker.EyeInfo}
     * per-tick step as mob eyes (fed the player's look + position deltas), so the behavior is identical:
     * it reacts to movement and settles to rest when the player stands still. It steps only while the
     * player holds a Googly Eye and starts at rest each time one is picked up.
     */
    private static final class HeldWobble {
        private final RandomSource rand = RandomSource.create();
        private GooglyTracker.EyeInfo eye;
        private double prevX;
        private double prevY;
        private double prevZ;

        void tick() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || !holdsGooglyEye(player)) {
                eye = null;
                return;
            }
            double mx = 0;
            double my = 0;
            double mz = 0;
            if (eye == null) {
                eye = new GooglyTracker.EyeInfo();
            } else {
                mx = player.getX() - prevX;
                my = player.getY() - prevY;
                mz = player.getZ() - prevZ;
            }
            prevX = player.getX();
            prevY = player.getY();
            prevZ = player.getZ();
            // A held eye plays no behavior, so the pupil has no spring (stiffness 0) — pure physics.
            eye.update(rand, player.getYHeadRot(), player.getXRot(), mx, my, mz, 0F, 0F, 0F);
        }

        void reset() {
            eye = null;
        }

        float x(float partialTick) {
            return eye == null ? 0F : Mth.lerp(partialTick, eye.prevDeltaX, eye.deltaX);
        }

        float y(float partialTick) {
            return eye == null ? 0F : Mth.lerp(partialTick, eye.prevDeltaY, eye.deltaY);
        }

        private static boolean holdsGooglyEye(LocalPlayer player) {
            return player.getMainHandItem().is(ModContent.GOOGLY_EYE.get())
                    || player.getOffhandItem().is(ModContent.GOOGLY_EYE.get());
        }
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
    public AppearanceOverride extractArgument(ItemStack stack) {
        return EyeItemProperties.get(stack);
    }

    @Override
    public void render(@Nullable AppearanceOverride argument, ItemDisplayContext ctx, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay, boolean hasFoil) {
        AppearanceOverride props = argument != null ? argument : AppearanceOverride.EMPTY;
        float[] cornea = props.cornea().orElse(EyeColor.WHITE).toArray();
        float[] iris = props.iris().orElse(EyeColor.BLACK).toArray();
        boolean glow = props.glow().orElse(false);

        float irisX = 0F;
        float irisY = 0F;
        if (isFirstPersonHand(ctx)) {
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            irisX = WOBBLE.x(partialTick);
            irisY = WOBBLE.y(partialTick);
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
        // Items have no placement, so they keep the standard thickness (depth multiplier 1).
        pose.scale(scale, scale, scale * ModelGooglyEye.BASE_DEPTH);

        GooglyEyeRenderer.drawEye(pose, m, buffer, light, overlay, cornea, iris, IRIS_SCALE, irisX, irisY, glow);
        pose.popPose();
    }
}
