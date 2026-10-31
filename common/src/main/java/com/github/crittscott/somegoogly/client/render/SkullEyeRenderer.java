package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import javax.annotation.Nullable;

/**
 * Submits the eyes of placed mob heads and head items. A placed head's eyes are chosen while its render state
 * is extracted ({@link #extract}) and kept on that state ({@link PlacedHolder}). Mixins bracket a placed head's
 * submission with {@link #begin} and a head item's with {@link #beginItem}, both ending in {@link #end}, and
 * call {@link #render} right after the head model is submitted, in the model's space. A placed head's eyes are
 * live ({@link SkullEyes}); an item's are drawn at rest, pupils centered, like the Googly Eye item's. Worn
 * heads share the model submission without either bracket, so they draw no eyes.
 *
 * <p>The eyes attach to the model's head part, which a vanilla head model shares with its mob, so the mob's
 * definition places them as they are. Honors the client display settings by the head's mob and the server's
 * {@code googlyEyesEnabled} switch.
 */
public final class SkullEyeRenderer {

    /** A head item's eyes: never simulated, so the pupil stays centered. */
    private static final GooglyTracker.EyeInfo REST = rest();

    /** The placed head's eyes being submitted, or {@code null} outside a placed head's submission. */
    @Nullable
    private static SkullEyes current;
    /** The head item being drawn, or {@code null} outside a head item's draw. */
    @Nullable
    private static ItemStack currentItem;
    private static float partialTick;

    private SkullEyeRenderer() {
    }

    /** An item render state's link to the stack it was last filled from, added by a Mixin. */
    public interface ItemHolder {

        /** The stack, or {@code null} when the state is clear. */
        @Nullable
        ItemStack somegoogly$item();

        void somegoogly$setItem(@Nullable ItemStack stack);
    }

    /** A placed head's render state's link to the eyes chosen when it was extracted, added by a Mixin. */
    public interface PlacedHolder {

        /** The eyes to draw, or {@code null} when the head shows none. */
        @Nullable
        SkullEyes somegoogly$eyes();

        float somegoogly$partialTick();

        void somegoogly$setEyes(@Nullable SkullEyes eyes, float partialTick);
    }

    /**
     * The eyes of the placed head {@code skull} to draw this frame, or {@code null} when it has none to show, as
     * its render state is extracted.
     */
    @Nullable
    public static SkullEyes extract(SkullBlockEntity skull) {
        ResourceLocation entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (!SkullEyeState.hasEyes(skull) || !mayShow(entity)) {
            return null;
        }
        SkullEyes eyes = ClientEyeRuntime.skullEyes(skull, entity);
        if (eyes != null) {
            eyes.markRendered(ClientEyeRuntime.clientTicks());
        }
        return eyes;
    }

    /** A placed head's submission starts. */
    public static void begin(PlacedHolder state) {
        current = state.somegoogly$eyes();
        partialTick = state.somegoogly$partialTick();
    }

    /** An item's submission starts; any but a head item's is ignored, since it never reaches {@link #render}. */
    public static void beginItem(@Nullable ItemStack stack) {
        currentItem = stack;
    }

    /** The placed head's or head item's submission ends. */
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
        if (model instanceof PiglinHeadModel piglin) {
            return piglin.head;
        }
        return null;
    }

    /** Whether eyes of {@code entity}'s kind of head may show on this client now. */
    private static boolean mayShow(@Nullable ResourceLocation entity) {
        return entity != null && !ClientConfig.DISABLE_GOOGLY_EYES.get() && ClientEyeConfigs.googlyEyesEnabled()
                && !ClientConfig.isEntityDisabled(entity);
    }

    /**
     * Submit the current placed head's or head item's eyes, if it has any to show, with {@code pose} in the space
     * {@code model} was just submitted in, the head's {@code packedLight}, and the head's {@code yRot} and
     * {@code mouthAnimation}. The model is posed only when it is drawn, after submission, so it is posed here
     * the same way first.
     */
    public static void render(PoseStack pose, SubmitNodeCollector collector, int packedLight, SkullModelBase model,
                              float yRot, float mouthAnimation) {
        ModelPart head = headPart(model);
        if (head == null) {
            return;
        }
        SkullEyes eyes = current;
        ItemStack stack = currentItem;
        if (eyes == null && stack == null) {
            return;
        }
        SkullModelBase.State modelState = new SkullModelBase.State();
        modelState.yRot = yRot;
        modelState.animationPos = mouthAnimation;
        model.setupAnim(modelState);
        if (eyes != null) {
            renderEyes(pose, collector, packedLight, head, eyes.helper, eyes.parts, eyes.eyes,
                    eyes.snapshot.properties(), partialTick);
        } else {
            ResourceLocation entity = SkullEyeState.entityFor(stack);
            Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
            if (snapshot == null || !mayShow(entity)) {
                return;
            }
            HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
            if (helper.hasConfig()) {
                renderEyes(pose, collector, packedLight, head, helper, SkullEyeState.headParts(helper), null,
                        snapshot.properties(), 0F);
            }
        }
    }

    /**
     * Submit the eyes of {@code helper}'s heads {@code parts} on the model's {@code head} part, each simulated by its
     * entry in {@code eyes}, or at rest when {@code eyes} is {@code null}.
     */
    private static void renderEyes(PoseStack pose, SubmitNodeCollector collector, int packedLight, ModelPart head,
                                   HeadInfo helper, int[] parts, @Nullable GooglyTracker.EyeInfo[][] eyes,
                                   AppearanceOverride overrides, float partialTick) {
        pose.pushPose();
        head.translateAndRotate(pose);
        for (int part : parts) {
            int eyeCount = helper.getEyeCount(part);
            for (int eye = 0; eye < eyeCount; eye++) {
                if (helper.getEyeScale(part, eye) > 0F) {
                    GooglyEyeRenderer.renderEye(pose, collector, packedLight, OverlayTexture.NO_OVERLAY,
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
