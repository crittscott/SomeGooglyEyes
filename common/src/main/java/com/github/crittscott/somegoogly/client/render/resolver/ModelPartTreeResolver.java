package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
 * <p>Resolving a token records the chain of parts from a subtree root down to the attach part; applying
 * that chain replays each part's {@code translateAndRotate} onto the live (this-frame, post-animation)
 * {@link PoseStack}, so the resulting pose carries every ancestor's animation. The descent mirrors
 * {@link ModelPart#visit}, except that it does <b>not</b> skip cube-less pivot/group parts, so eyes can
 * attach to a named empty joint.
 */
abstract class ModelPartTreeResolver implements EyeAttachmentResolver {

    /**
     * A subtree root paired with the name it should appear under (roots have no intrinsic stable name)
     * and an optional transform to replay on the {@link PoseStack} before descending into it (used by
     * {@link AgeableListResolver} to reproduce a baby-only wrap the model applies outside its part tree).
     */
    protected record NamedRoot(String name, ModelPart part, @Nullable Consumer<PoseStack> preTransform) {
        NamedRoot(String name, ModelPart part) {
            this(name, part, null);
        }
    }

    /** The subtrees to walk for this model, in a stable order. Only called when {@link #handles} is true. */
    protected abstract List<NamedRoot> roots(EntityModel<?> model);

    /** Name a part group {@code base}, {@code base1}, {@code base2}, … in iteration order. */
    protected static void index(List<NamedRoot> roots, String base, Iterable<ModelPart> parts,
                                Consumer<PoseStack> preTransform) {
        int i = 0;
        for (ModelPart part : parts) {
            roots.add(new NamedRoot(i == 0 ? base : base + i, part, preTransform));
            i++;
        }
    }

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
        // First part (pre-order) whose path suffix-matches the stored token — the same order the search
        // below follows, so canonical naming and attachment agree.
        for (String path : enumerateParts(model)) {
            if (EyeAttachmentResolver.pathMatches(storedToken, path)) {
                return path;
            }
        }
        return storedToken; // no part matches; leave the token as authored
    }

    /** The parts from a subtree root down to the attach part, inclusive, in the order they transform. */
    private record PartChain(@Nullable Consumer<PoseStack> preTransform, ModelPart[] chain) implements Attachment {
        @Override
        public boolean apply(PoseStack poseStack) {
            if (preTransform != null) {
                preTransform.accept(poseStack);
            }
            for (ModelPart part : chain) {
                part.translateAndRotate(poseStack);
            }
            return true;
        }
    }

    @Override
    @Nullable
    public Attachment resolve(EntityModel<?> model, String partToken) {
        if (!handles(model)) {
            return null;
        }
        ArrayDeque<ModelPart> chain = new ArrayDeque<>();
        for (NamedRoot root : roots(model)) {
            if (search("", root.name(), root.part(), partToken, chain)) {
                return new PartChain(root.preTransform(), chain.toArray(new ModelPart[0]));
            }
        }
        return null;
    }

    /**
     * Depth-first pre-order over the {@code children} map, in the same root order and the same path
     * spelling {@link #enumerate} uses, so a token attaches to the part {@link #canonicalToken} names. A
     * matching part is not descended into, so an ancestor {@code head} wins over a descendant {@code head}.
     *
     * <p>On success {@code chain} holds root→part inclusive; on failure it is left as it was found.
     */
    private static boolean search(String prefix, String name, ModelPart part, String token,
                                  ArrayDeque<ModelPart> chain) {
        String path = prefix.isEmpty() ? name : prefix + "/" + name;
        chain.addLast(part);
        if (EyeAttachmentResolver.pathMatches(token, path)) {
            return true;
        }
        for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
            if (search(path, child.getKey(), child.getValue(), token, chain)) {
                return true;
            }
        }
        chain.removeLast();
        return false;
    }
}
