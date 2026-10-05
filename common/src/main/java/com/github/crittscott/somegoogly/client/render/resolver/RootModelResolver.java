package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolver for every model whose geometry hangs off {@link EntityModel#root()} — all vanilla models and
 * any third-party model built the vanilla way. It walks that tree by its name → child map, naming the root
 * {@code root}, so tokens are slash-joined paths like {@code root/body/head}. Matching is a normalized
 * <b>suffix</b> match (see {@link EyeAttachmentResolver#pathMatches}), so a stored {@code head} still
 * attaches and a stored {@code body/head} disambiguates two same-named parts under different parents.
 *
 * <p>Resolving a token records the chain of parts from the root down to the attach part; applying that
 * chain replays each part's {@code translateAndRotate} onto the live (this-frame, post-animation)
 * {@link PoseStack}, so the resulting pose carries every ancestor's animation and scale. Baby models are
 * separate model instances whose age scaling lives in their part poses, so the same replay places a
 * baby's eyes. The descent mirrors {@link ModelPart#visit}, except that it does <b>not</b> skip cube-less
 * pivot/group parts, so eyes can attach to a named empty joint.
 *
 * <p>It handles every model, so {@link Resolvers} tries it last.
 */
public class RootModelResolver implements EyeAttachmentResolver {

    private static final String ROOT = "root";

    @Override
    public boolean handles(EntityModel<?> model) {
        return true;
    }

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        List<String> out = new ArrayList<>();
        enumerate("", ROOT, model.root(), out);
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
        // First part (pre-order) whose path suffix-matches the stored token — the same order the search
        // below follows, so canonical naming and attachment agree.
        for (String path : enumerateParts(model)) {
            if (EyeAttachmentResolver.pathMatches(storedToken, path)) {
                return path;
            }
        }
        return storedToken; // no part matches; leave the token as authored
    }

    /** The parts from the root down to the attach part, inclusive, in the order they transform. */
    private record PartChain(ModelPart[] chain) implements Attachment {
        @Override
        public boolean apply(PoseStack poseStack) {
            for (ModelPart part : chain) {
                part.translateAndRotate(poseStack);
            }
            return true;
        }
    }

    @Override
    @Nullable
    public Attachment resolve(EntityModel<?> model, String partToken) {
        ArrayDeque<ModelPart> chain = new ArrayDeque<>();
        return search("", ROOT, model.root(), partToken, chain)
                ? new PartChain(chain.toArray(new ModelPart[0]))
                : null;
    }

    /**
     * Depth-first pre-order over the {@code children} map, in the same order and the same path spelling
     * {@link #enumerate} uses, so a token attaches to the part {@link #canonicalToken} names. A matching
     * part is not descended into, so an ancestor {@code head} wins over a descendant {@code head}.
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
