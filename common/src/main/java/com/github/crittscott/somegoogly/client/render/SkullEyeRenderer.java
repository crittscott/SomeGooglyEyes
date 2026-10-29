package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.github.crittscott.somegoogly.client.SkullEyes;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import javax.annotation.Nullable;

/**
 * Draws the eyes of placed mob heads and head items. Each loader's Mixins bracket a placed head's draw with
 * {@link #begin} and a head item's with {@link #beginItem}, both ending in {@link #end}, and call
 * {@link #render} right after the head model is drawn, in the model's space. A placed head's eyes are live
 * ({@link SkullEyes}); an item's are drawn at rest, pupils centered, like the Googly Eye item's. Worn heads
 * share the model draw without either bracket, so they draw no eyes.
 *
 * <p>The eyes attach to the model's head part, which a head model shares with its mob, so the mob's definition
 * places them. Honors the client display settings by the head's mob and the server's {@code googlyEyesEnabled}
 * switch.
 */
public final class SkullEyeRenderer {

    // Like the mob layers' model, one instance whose iris is moved before each draw on the render thread.
    private static final ModelGooglyEye MODEL = new ModelGooglyEye();
    /** A head item's eyes: never simulated, so the pupil stays centered. */
    private static final GooglyTracker.EyeInfo REST = rest();

    /** The placed head being drawn, or {@code null} outside a placed head's draw. */
    @Nullable
    private static SkullBlockEntity current;
    /** The head item being drawn, or {@code null} outside a head item's draw. */
    @Nullable
    private static ItemStack currentItem;
    private static float partialTick;

    private SkullEyeRenderer() {
    }

    /** A placed head's draw starts. */
    public static void begin(SkullBlockEntity skull, float partialTick) {
        current = skull;
        SkullEyeRenderer.partialTick = partialTick;
    }

    /** A head item's draw starts; any other item's draw is ignored, since it never reaches {@link #render}. */
    public static void beginItem(ItemStack stack) {
        currentItem = stack;
    }

    /** The placed head's or head item's draw ends. */
    public static void end() {
        current = null;
        currentItem = null;
    }

    /** The head part of a model eyes can attach to, or {@code null} for any other head model (the dragon's). */
    @Nullable
    private static ModelPart headPart(SkullModelBase model) {
        if (model instanceof SkullModel skull) {
            return skull.head;
        }
        return model instanceof PiglinHeadModel piglin ? piglin.head : null;
    }

    /** Whether eyes of {@code entity}'s kind of head may show on this client now. */
    private static boolean mayShow(@Nullable ResourceLocation entity) {
        return entity != null && !ClientConfig.DISABLE_GOOGLY_EYES.get() && ClientEyeConfigs.googlyEyesEnabled()
                && !ClientConfig.isEntityDisabled(entity);
    }

    /**
     * Draw the current placed head's or head item's eyes, if it has any to show, with {@code pose} in the space
     * {@code model} was just drawn in and the head's {@code packedLight}.
     */
    public static void render(PoseStack pose, MultiBufferSource bufferSource, int packedLight, SkullModelBase model) {
        ModelPart head = headPart(model);
        if (head == null) {
            return;
        }
        SkullBlockEntity skull = current;
        ItemStack stack = currentItem;
        if (skull != null) {
            ResourceLocation entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
            if (!SkullEyeState.hasEyes(skull) || !mayShow(entity)) {
                return;
            }
            SkullEyes eyes = ClientEyeRuntime.skullEyes(skull, entity);
            if (eyes != null) {
                eyes.markRendered(ClientEyeRuntime.clientTicks());
                renderEyes(pose, bufferSource, packedLight, head, eyes.helper, eyes.parts, eyes.eyes,
                        eyes.snapshot.properties(), partialTick);
            }
        } else if (stack != null) {
            ResourceLocation entity = SkullEyeState.entityFor(stack);
            Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
            if (snapshot == null || !mayShow(entity)) {
                return;
            }
            HeadInfo helper = ClientEyeConfigs.resolve(entity, false, snapshot.variantRoll());
            if (helper.hasConfig()) {
                renderEyes(pose, bufferSource, packedLight, head, helper, SkullEyeState.headParts(helper), null,
                        snapshot.properties(), 0F);
            }
        }
    }

    /**
     * Draw the eyes of {@code helper}'s heads {@code parts} on the model's {@code head} part, each simulated by its
     * entry in {@code eyes}, or at rest when {@code eyes} is {@code null}.
     */
    private static void renderEyes(PoseStack pose, MultiBufferSource bufferSource, int packedLight, ModelPart head,
                                   HeadInfo helper, int[] parts, @Nullable GooglyTracker.EyeInfo[][] eyes,
                                   AppearanceOverride overrides, float partialTick) {
        pose.pushPose();
        head.translateAndRotate(pose);
        for (int part : parts) {
            int eyeCount = helper.getEyeCount(part);
            for (int eye = 0; eye < eyeCount; eye++) {
                if (helper.getEyeScale(part, eye) > 0F) {
                    GooglyEyeRenderer.renderEye(pose, MODEL, bufferSource, packedLight, OverlayTexture.NO_OVERLAY,
                            eyes == null ? REST : eyes[part][eye], helper, overrides, part, eye, partialTick);
                }
            }
        }
        pose.popPose();
    }

    private static GooglyTracker.EyeInfo rest() {
        GooglyTracker.EyeInfo rest = new GooglyTracker.EyeInfo();
        rest.deltaY = 0F;
        rest.prevDeltaY = 0F;
        return rest;
    }
}
