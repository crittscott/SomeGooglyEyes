package com.github.crittscott.somegoogly.client.render.resolver;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Catch-all resolver for any vanilla {@link EntityModel} that isn't a
 * {@link net.minecraft.client.model.HierarchicalModel} or {@link net.minecraft.client.model.AgeableListModel}
 * — and the final fallback for everything else. It finds the model's top-level {@link ModelPart}s by
 * reflecting fields <b>by type</b> (never by name — those are SRG-mangled in production) and keeping only
 * those that aren't a descendant of another collected part, so genuine roots aren't double-counted.
 *
 * <p>Roots get positional names ({@code #0}, {@code #1}, … in superclass-first field order), but every
 * descendant below a root is named by its real {@code children}-map key (made readable by the mod's access
 * transformer). So a model that nests named parts under a root yields stable paths like {@code #0/head1}
 * (recoverable where the old index-only fallback could only ever say {@code #0}); a model whose head is a
 * bare nameless root still resolves as {@code #0}. Matching is the shared suffix match, so a stored
 * {@code head1} attaches regardless of which positional root it hangs under.
 *
 * <p>Replaces the former {@code ReflectionResolver}: same root-by-type reflection, but now with real
 * descendant names and proper per-level pose accumulation from {@link ModelPartTreeResolver}.
 */
public class ChildMapResolver extends ModelPartTreeResolver {

    // Roots resolved per model instance (models are singletons per renderer; render thread only).
    private static final Map<EntityModel<?>, List<NamedRoot>> CACHE = new WeakHashMap<>();

    @Override
    public boolean handles(EntityModel<?> model) {
        return true; // catch-all; the named-model resolvers are tried first
    }

    @Override
    protected List<NamedRoot> roots(EntityModel<?> model) {
        return CACHE.computeIfAbsent(model, ChildMapResolver::reflectRoots);
    }

    /**
     * Collect {@link ModelPart} fields by type (superclass-first, so a humanoid/quadruped head lands at
     * index 0), drop any that are a descendant of another collected part, and name the survivors
     * positionally. Every access is guarded, so an unreflectable model just yields no roots (no eyes) —
     * never a crash.
     */
    private static List<NamedRoot> reflectRoots(EntityModel<?> model) {
        Set<ModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ModelPart> ordered = new ArrayList<>();
        for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType() != ModelPart.class) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.get(model) instanceof ModelPart part && seen.add(part)) {
                        ordered.add(part);
                    }
                } catch (Throwable accessDenied) {
                    // Module/access restriction or anything else: skip this field.
                }
            }
        }

        Set<ModelPart> descendants = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart part : ordered) {
            collectDescendants(part, descendants);
        }

        List<NamedRoot> roots = new ArrayList<>();
        int i = 0;
        for (ModelPart part : ordered) {
            if (!descendants.contains(part)) {
                roots.add(new NamedRoot("#" + (i++), part));
            }
        }
        return roots;
    }

    private static void collectDescendants(ModelPart part, Set<ModelPart> out) {
        for (ModelPart child : part.children.values()) {
            if (out.add(child)) {
                collectDescendants(child, out);
            }
        }
    }
}
