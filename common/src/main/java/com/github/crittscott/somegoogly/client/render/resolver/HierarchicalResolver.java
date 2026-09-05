package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AgeableHierarchicalModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Resolver for the {@link HierarchicalModel} family (a named root + child maps). The whole tree hangs off
 * the single public {@link HierarchicalModel#root()}, which we walk as the {@code root} subtree via the
 * shared {@link ModelPartTreeResolver} machinery — so tokens are paths like {@code root/body/head} and a
 * stored {@code head} still suffix-matches.
 *
 * <p>This resolver must run before {@link AgeableListResolver} and {@link ChildMapResolver} because a
 * hierarchical model exposes its root cleanly and unambiguously. Walking the {@code children} map directly
 * (not {@code root().visit()}, which skips cube-less parts) also lets eyes attach to a cube-less pivot/group
 * joint.
 *
 * <p>{@link AgeableHierarchicalModel} and {@link CamelModel} each wrap their whole {@code root()} render in
 * a baby-only scale/translate applied and popped entirely inside their own {@code renderToBuffer} —
 * invisible to a render layer's {@link PoseStack} otherwise, the {@code HierarchicalModel} counterpart of
 * the wrap {@link AgeableListResolver} replays for its own family. {@link #youngWrap} replays it so a young
 * Sniffer or Camel's attach points land where the model actually draws them.
 */
public class HierarchicalResolver extends ModelPartTreeResolver {

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof HierarchicalModel;
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        HierarchicalModel<?> hierarchical = (HierarchicalModel<?>) model;
        return List.of(new NamedRoot("root", hierarchical.root(), youngWrap(hierarchical)));
    }

    /**
     * {@code null} for the common case (no wrap to replay). {@code young} is re-checked on every call
     * rather than baked in when this closure is built, because {@link AttachmentCache#ATTACHMENTS} caches the
     * resolved chain per (model, token) and the same model instance renders both baby and adult entities
     * of its type.
     */
    @Nullable
    private static Consumer<PoseStack> youngWrap(HierarchicalModel<?> model) {
        if (model instanceof AgeableHierarchicalModel<?> ageable) {
            float scale = ageable.youngScaleFactor;
            float yOffset = ageable.bodyYOffset;
            return poseStack -> {
                if (!ageable.young) {
                    return;
                }
                poseStack.scale(scale, scale, scale);
                poseStack.translate(0.0F, yOffset / 16.0F, 0.0F);
            };
        }
        if (model instanceof CamelModel<?> camel) {
            return poseStack -> {
                if (!camel.young) {
                    return;
                }
                poseStack.scale(0.45F, 0.45F, 0.45F);
                poseStack.translate(0.0F, 1.834375F, 0.0F);
            };
        }
        return null;
    }
}
