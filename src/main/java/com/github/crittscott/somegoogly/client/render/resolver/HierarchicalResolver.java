package com.github.crittscott.somegoogly.client.render.resolver;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;

import java.util.List;

/**
 * Resolver for the {@link HierarchicalModel} family (a named root + child maps). The whole tree hangs off
 * the single public {@link HierarchicalModel#root()}, which we walk as the {@code root} subtree via the
 * shared {@link ModelPartTreeResolver} machinery — so tokens are paths like {@code root/body/head} and a
 * stored {@code head} still suffix-matches.
 *
 * <p>Tried first (before {@link AgeableListResolver}/{@link ChildMapResolver}) because a hierarchical
 * model exposes its root cleanly and unambiguously. Walking the {@code children} map directly (not
 * {@code root().visit()}, which skips cube-less parts) also lets eyes attach to a cube-less pivot/group
 * joint.
 */
public class HierarchicalResolver extends ModelPartTreeResolver {

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof HierarchicalModel;
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        return List.of(new NamedRoot("root", ((HierarchicalModel<?>) model).root()));
    }
}
