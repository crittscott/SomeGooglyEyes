package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

/**
 * Strategy for getting from an entity model's base render pose into the animated space of a named
 * attachment part, so eyes can be drawn relative to it.
 *
 * <p>Different model frameworks expose their part trees differently (see {@link HierarchicalResolver}
 * and {@link ReflectionResolver}); each converges on the same contract here. Implementations must use
 * <b>obfuscation-safe</b> handles only — string part names / stable indices, never obfuscated fields.
 */
public interface EyeAttachmentResolver {

    /**
     * List selectable attachment tokens for this model, in a stable order (used by the part picker).
     * Default: none (resolver doesn't support enumeration / picker authoring yet).
     */
    default java.util.List<String> enumerateParts(EntityModel<?> model) {
        return java.util.List.of();
    }

    /** Whether this resolver knows how to walk the given model's part tree. */
    boolean handles(EntityModel<?> model);

    /** Normalizes a token/part-name so camelCase field names match snake_case child-map keys. */
    static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Move {@code poseStack} into the named part's current (this-frame, post-animation) space.
     * The caller is responsible for {@code pushPose()}/{@code popPose()} around this call.
     *
     * @param partToken the configured attachment token (a string part name, possibly camelCase)
     * @return {@code true} if the part was found and the pose moved; {@code false} otherwise (caller
     *         should skip drawing for this head)
     */
    boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken);
}
