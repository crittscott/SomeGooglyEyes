package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.client.GooglyTracker;
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
 * Draws the eyes of placed mob heads and head items. Mixins bracket a placed head's draw with
 * {@link ClientEyeRuntime#beginHead} and an item render state's with {@link ClientEyeRuntime#beginItemState}, both
 * closed by {@link ClientEyeRuntime#endItemDraw}, and call {@link #render} right after the head model is drawn, in
 * the model's space. A placed head's eyes are live ({@link PresentationEyes}); GUI, hand, and framed head items use
 * their presentation's pupils and expressions; other items rest. Worn heads share the model draw without
 * either bracket, so they draw no eyes.
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
        return entity != null && ClientEyeConfigs.eyesVisible(entity);
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
        ClientEyeRuntime.DrawContext draw = ClientEyeRuntime.currentDraw();
        SkullBlockEntity skull = draw.block();
        ItemStack stack = draw.item();
        if (skull != null) {
            ResourceLocation entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
            if (!SkullEyeState.hasEyes(skull) || !mayShow(entity)) {
                return;
            }
            PresentationEyes eyes = ClientEyeRuntime.skullEyes(skull, entity);
            if (eyes != null) {
                eyes.markRendered(ClientEyeRuntime.clientTicks());
                renderEyes(pose, bufferSource, packedLight, head, eyes.helper, eyes.parts, eyes.eyes,
                        eyes.snapshot.properties(), draw.partialTick());
            }
        } else if (stack != null) {
            ResourceLocation entity = SkullEyeState.entityFor(stack);
            if (!mayShow(entity)) {
                return;
            }
            PresentationEyes.Draw itemDraw = draw.draw();
            PresentationEyes framed = itemDraw.eyes();
            if (framed != null) {
                renderEyes(pose, bufferSource, packedLight, head, framed.helper, framed.parts, framed.eyes,
                        framed.properties, itemDraw.partialTick());
                return;
            }
            Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
            if (snapshot == null) {
                return;
            }
            HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
            if (helper.hasConfig()) {
                renderEyes(pose, bufferSource, packedLight, head, helper, SkullEyeState.headParts(helper), null,
                        snapshot.properties(), itemDraw.partialTick());
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
            GooglyEyeRenderer.renderHead(pose, bufferSource, packedLight, OverlayTexture.NO_OVERLAY,
                    eyes == null ? null : eyes[part], helper, overrides, part, partialTick);
        }
        pose.popPose();
    }
}
