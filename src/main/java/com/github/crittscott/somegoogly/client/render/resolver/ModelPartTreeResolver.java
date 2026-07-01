package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared base for resolvers that walk a vanilla {@link ModelPart} tree by its name → child map. The three
 * concrete subclasses differ only in <i>where the roots come from</i> and what those roots are called:
 *
 * <ul>
 *   <li>{@link HierarchicalResolver} — the single public {@link net.minecraft.client.model.HierarchicalModel#root()},
 *       named {@code root}.</li>
 *   <li>{@link AgeableListResolver} — the {@code headParts()}/{@code bodyParts()} groups, named
 *       {@code head}/{@code body} (stable across obfuscation, unlike the SRG-mangled field names).</li>
 *   <li>{@link ChildMapResolver} — reflected top-level {@code ModelPart} fields, named positionally
 *       ({@code #0}, {@code #1}, …) since a root field has no obfuscation-stable name.</li>
 * </ul>
 *
 * <p>Reading {@link ModelPart}'s {@code children} map directly (made accessible by the mod's access
 * transformer) is what lets these recover the modder's real <b>descendant</b> names ({@code head1},
 * {@code body1}, …) on any model — not just hierarchical ones. Tokens are slash-joined paths
 * ({@code root/body/head}); matching is a normalized <b>suffix</b> match (see
 * {@link EyeAttachmentResolver#pathMatches}), so a stored {@code head} still attaches and a stored
 * {@code body/head} disambiguates two same-named parts under different parents. A bare {@code #N} is just
 * the path of a nameless positional root (from {@link ChildMapResolver}); it matches the same way.
 *
 * <p>The walk mirrors {@link ModelPart#visit}: it applies each part's {@code translateAndRotate} onto the
 * live (this-frame, post-animation) {@link PoseStack} before descending, so the captured pose includes
 * every ancestor's animation. Unlike {@code visit} it does <b>not</b> skip cube-less pivot/group parts, so
 * eyes can attach to a named empty joint (the limitation the old visit-based resolver documented).
 */
abstract class ModelPartTreeResolver implements EyeAttachmentResolver {

    /** A subtree root paired with the name it should appear under (roots have no intrinsic stable name). */
    protected record NamedRoot(String name, ModelPart part) {
    }

    /** The subtrees to walk for this model, in a stable order. Only called when {@link #handles} is true. */
    protected abstract List<NamedRoot> roots(EntityModel<?> model);

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        if (!handles(model)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (NamedRoot root : roots(model)) {
            enumerate("", root.name(), root.part(), out);
        }
        return out;
    }

    private static void enumerate(String prefix, String name, ModelPart part, List<String> out) {
        String path = prefix.isEmpty() ? name : prefix + "/" + name;
        out.add(path);
        for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
            enumerate(path, child.getKey(), child.getValue(), out);
        }
    }

    @Override
    public String canonicalToken(EntityModel<?> model, String storedToken) {
        if (!handles(model)) {
            return storedToken;
        }
        // First part (pre-order) whose path suffix-matches the stored token — the same order the pose walk
        // below follows, so canonical naming and attachment agree.
        for (String path : enumerateParts(model)) {
            if (EyeAttachmentResolver.pathMatches(storedToken, path)) {
                return path;
            }
        }
        return storedToken; // no part matches; leave the token as authored
    }

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        if (!handles(model)) {
            return false;
        }
        Matrix4f[] capPose = new Matrix4f[1];
        Matrix3f[] capNormal = new Matrix3f[1];
        for (NamedRoot root : roots(model)) {
            walk(poseStack, "", root.name(), root.part(), partToken, capPose, capNormal);
            if (capPose[0] != null) {
                break;
            }
        }
        if (capPose[0] == null) {
            return false;
        }
        poseStack.last().pose().set(capPose[0]);
        poseStack.last().normal().set(capNormal[0]);
        return true;
    }

    private static void walk(PoseStack pose, String prefix, String name, ModelPart part, String token,
                             Matrix4f[] capPose, Matrix3f[] capNormal) {
        if (capPose[0] != null) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        String path = prefix.isEmpty() ? name : prefix + "/" + name;
        if (EyeAttachmentResolver.pathMatches(token, path)) {
            // Copy: the pose entry is reused/popped after this returns.
            capPose[0] = new Matrix4f(pose.last().pose());
            capNormal[0] = new Matrix3f(pose.last().normal());
        } else {
            for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
                walk(pose, path, child.getKey(), child.getValue(), token, capPose, capNormal);
            }
        }
        pose.popPose();
    }
}
