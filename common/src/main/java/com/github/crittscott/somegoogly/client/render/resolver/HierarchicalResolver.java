package com.github.crittscott.somegoogly.client.render.resolver;

import com.github.crittscott.somegoogly.client.compat.ThirdPartyModelWraps;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.AgeableHierarchicalModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;

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
 * the wrap {@link AgeableListResolver} replays for its own family. {@link ThirdPartyModelWraps#groupTransform} replays it so a young
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
        return List.of(new NamedRoot(EyeConfigModel.PART_ROOT, hierarchical.root(), ThirdPartyModelWraps.groupTransform(hierarchical, EyeConfigModel.PART_ROOT)));
    }

}
