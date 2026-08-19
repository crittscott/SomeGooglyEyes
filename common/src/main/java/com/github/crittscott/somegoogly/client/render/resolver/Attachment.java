package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * A resolved attach point: the ordered transforms that carry a pose from the model's base render space
 * into a named part's animated space. Resolving one means finding the part (a tree search by string
 * name); applying it means replaying the transforms of the part and each of its ancestors. Only the
 * first is expensive, and it depends on nothing that changes, so {@link EyeAttachmentResolver} resolves
 * once per (model, token) and applies the result every frame.
 */
public interface Attachment {

    /**
     * Move {@code poseStack} into the part's current (this-frame, post-animation) space. The caller owns
     * the surrounding {@code pushPose()}/{@code popPose()}.
     *
     * @return {@code false} if a transform could not be applied (a reflected model family whose handles
     *         failed); the caller should skip drawing.
     */
    boolean apply(PoseStack poseStack);
}
