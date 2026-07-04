package com.github.crittscott.somegoogly.client.render.resolver;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolver for the {@link AgeableListModel} family — the base of most vanilla mob models (humanoids,
 * quadrupeds, and many others). These keep no named root, but they <i>do</i> expose their parts in two
 * groups, {@code headParts()} and {@code bodyParts()} (made accessible by the mod's access transformer).
 * We name those roots {@code head}/{@code body} (the first of each group) and {@code head1}, {@code body1},
 * … for any extras, then walk each subtree's {@code children} map for deeper real names.
 *
 * <p>This is what gives a cow or a player the stable token {@code head} instead of a positional {@code #0}
 * — the head is a root field whose own name is SRG-mangled in production, so only this group accessor
 * recovers it reliably. Tried after {@link HierarchicalResolver} (named-root models keep their cleaner
 * path) and before {@link ChildMapResolver} (the positional catch-all).
 *
 * <p>Known limit: {@code AgeableListModel}'s baby scale/offset is applied outside the part tree, so it is
 * not reflected in the captured pose — baby mobs may be slightly mis-placed. Pre-existing and orthogonal.
 */
public class AgeableListResolver extends ModelPartTreeResolver {

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof AgeableListModel;
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        AgeableListModel<?> ageable = (AgeableListModel<?>) model;
        List<NamedRoot> roots = new ArrayList<>();
        index(roots, "head", ageable.headParts());
        index(roots, "body", ageable.bodyParts());
        return roots;
    }

    /** Name a part group {@code base}, {@code base1}, {@code base2}, … in iteration order. */
    private static void index(List<NamedRoot> roots, String base, Iterable<ModelPart> parts) {
        int i = 0;
        for (ModelPart part : parts) {
            roots.add(new NamedRoot(i == 0 ? base : base + i, part));
            i++;
        }
    }
}
