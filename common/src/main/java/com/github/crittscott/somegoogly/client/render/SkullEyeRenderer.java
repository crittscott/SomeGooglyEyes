package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * Submits the eyes of placed mob heads and head items. A placed head's eyes are chosen while its render state
 * is extracted ({@link #extract}) and kept on that state ({@link PlacedHolder}). Mixins bracket a placed head's
 * submission with {@link ClientEyeRuntime#beginHead} and an item render state's with
 * {@link ClientEyeRuntime#beginItemState}, both closed by {@link ClientEyeRuntime#endItemDraw}, and call
 * {@link #render} as the head model is submitted, in the model's space; it reads that draw context while the head
 * is submitted, never later. A placed head's eyes are live ({@link PresentationEyes}). Head items in the GUI, in
 * third-person hands, or in frames have independent pupils and ambient expressions. Unanimated items have
 * centered pupils. Worn heads share the model submission outside both brackets, so they draw no eyes.
 *
 * <p>The eyes attach to the model's head part, which a vanilla head model shares with its mob, so the mob's
 * definition places them as they are. Honors the client display settings by the head's mob and the server's
 * {@code googlyEyesEnabled} switch.
 */
public final class SkullEyeRenderer {

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
        PresentationEyes somegoogly$eyes();

        float somegoogly$partialTick();

        void somegoogly$setEyes(@Nullable PresentationEyes eyes, float partialTick);
    }

    /**
     * The eyes of the placed head {@code skull} to draw this frame, or {@code null} when it has none to show, as
     * its render state is extracted.
     */
    @Nullable
    public static PresentationEyes extract(SkullBlockEntity skull) {
        Identifier entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (!SkullEyeState.hasEyes(skull) || !mayShow(entity)) {
            return null;
        }
        PresentationEyes eyes = ClientEyeRuntime.skullEyes(skull, entity);
        if (eyes != null) {
            eyes.markRendered(ClientEyeRuntime.clientTicks());
        }
        return eyes;
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
    private static boolean mayShow(@Nullable Identifier entity) {
        return entity != null && ClientEyeConfigs.eyesVisible(entity);
    }

    /**
     * Submit the current placed head's or head item's eyes, if it has any to show, with {@code pose} in the model's
     * submission space, its {@code packedLight}, and its extracted model state. The model is posed only when it is drawn, after submission, so it is posed here
     * the same way first.
     */
    public static void render(PoseStack pose, SubmitNodeCollector collector, int packedLight, SkullModelBase model,
                              SkullModelBase.State modelState) {
        ModelPart head = headPart(model);
        if (head == null) {
            return;
        }
        ClientEyeRuntime.DrawContext draw = ClientEyeRuntime.currentDraw();
        PresentationEyes eyes = draw.head();
        ItemStack stack = draw.item();
        if (eyes == null && stack == null) {
            return;
        }
        model.setupAnim(modelState);
        if (eyes != null) {
            renderEyes(pose, collector, packedLight, head, eyes.helper, eyes.parts, eyes.eyes,
                    eyes.snapshot.properties(), draw.partialTick());
        } else {
            Identifier entity = SkullEyeState.entityFor(stack);
            if (!mayShow(entity)) {
                return;
            }
            PresentationEyes.Draw itemDraw = draw.draw();
            PresentationEyes animated = itemDraw.eyes();
            if (animated != null) {
                renderEyes(pose, collector, packedLight, head, animated.helper, animated.parts, animated.eyes,
                        animated.properties, itemDraw.partialTick());
                return;
            }
            Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
            if (snapshot != null) {
                HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
                if (helper.hasConfig()) {
                    renderEyes(pose, collector, packedLight, head, helper, SkullEyeState.headParts(helper), null,
                            snapshot.properties(), 0F);
                }
            }
        }
    }

    /**
     * Submit the eyes of {@code helper}'s heads {@code parts} on the model's {@code head} part, each simulated by its
     * entry in {@code eyes}, or at rest when {@code eyes} is {@code null}.
     */
    private static void renderEyes(PoseStack pose, SubmitNodeCollector collector, int packedLight, ModelPart head,
                                   HeadInfo helper, int[] parts, GooglyTracker.EyeInfo @Nullable [][] eyes,
                                   AppearanceOverride overrides, float partialTick) {
        pose.pushPose();
        head.translateAndRotate(pose);
        for (int part : parts) {
            GooglyEyeRenderer.renderHead(pose, collector, packedLight, OverlayTexture.NO_OVERLAY,
                    eyes == null ? null : eyes[part], helper, overrides, part, partialTick);
        }
        pose.popPose();
    }
}
