package com.github.crittscott.somegoogly.client.render.resolver;

import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shared machinery for resolvers over reflected "box-tree" model families — external mods whose
 * models keep their own box hierarchy (a flat all-boxes list plus parent pointers and a per-box pose
 * transform) instead of vanilla {@code ModelPart}s. {@link AdvancedModelBoxResolver} and
 * {@link LLibraryResolver} are the concrete families; each supplies only the reflective handles,
 * while the token/path/index logic lives here so the families can't drift apart.
 *
 * <p>Tokens are the same full slash paths the vanilla resolvers speak — the box's parent chain,
 * suffix-matched via {@link EyeAttachmentResolver#pathMatches}, so a stored {@code Head} attaches to
 * {@code Tail_1/Body/Head} and a path disambiguates same-named boxes under different parents. Each
 * segment comes from three sources, in priority order:
 * <ol>
 *   <li>the box's own name, where the family has one ({@link #intrinsicName});</li>
 *   <li>the name of the model's own Java field holding the box. Mod classes are never
 *       obfuscation-mangled (only Minecraft/Forge classes are), so field names are as stable as box
 *       names;</li>
 *   <li>the positional {@code #N} (the box's index in the family's all-boxes list), for a box with no
 *       name and no field. It appears as a normal path segment and suffix-matches like any other.</li>
 * </ol>
 */
abstract class ReflectedBoxResolver implements EyeAttachmentResolver {

    // Indexed per model instance (models are singletons per renderer; render thread only). Both families'
    // boxes hold a reference back to their model, so an entry keeps its own weak key alive and only
    // clearModelCache() actually reclaims it.
    private final Map<EntityModel<?>, ModelIndex> cache = new WeakHashMap<>();
    // Models whose reflection failed draw no eyes until the next reset; the family's other models go on.
    private final Set<EntityModel<?>> failedModels = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void clearModelCache() {
        cache.clear();
        failedModels.clear();
    }

    /** Give up on {@code model} until the next reset and log the failure once. */
    private void fail(String operation, EntityModel<?> model, Object subject, Throwable failure) {
        failedModels.add(model);
        ClientIntegrationFailures.warnOnce(familyLabel(), operation,
                subject.getClass().getName(), failure);
    }

    /** Whether reflection on {@code model} has failed since the last reset; other models are unaffected. */
    protected final boolean failed(EntityModel<?> model) {
        return failedModels.contains(model);
    }

    /**
     * The family's name in logs. Defaults to the resolver's class name, which one resolver class
     * serving several libraries must override so its warnings say which one failed.
     */
    protected String familyLabel() {
        return getClass().getSimpleName();
    }

    /** Per-model index: the family's boxes, with an index-aligned path token each. */
    private record ModelIndex(List<Object> parts, List<String> paths) {
        static final ModelIndex EMPTY = new ModelIndex(List.of(), List.of());
    }

    /** Whether the reflective handles all loaded (the family's classes are present and API-shaped as expected). */
    protected abstract boolean available();

    /** The family's box class, for instance checks and the field-name fallback scan. */
    protected abstract Class<?> boxClass();

    /** The family's iterable of all the model's boxes, in its stable order. */
    protected abstract Object rawParts(EntityModel<?> model) throws ReflectiveOperationException;

    /** The box's author-set name, or {@code ""} when the family has none / it is unset. */
    protected abstract String intrinsicName(Object part);

    /** The box's parent, or {@code null} for a root. */
    protected abstract Object rawParent(Object part) throws ReflectiveOperationException;

    /** Apply the box's local pose transform (the family's translate+rotate). */
    protected abstract void rawTransform(Object part, PoseStack poseStack) throws ReflectiveOperationException;

    /** All of the model's boxes in the family's stable order; empty on any reflection failure. */
    private List<Object> collectParts(EntityModel<?> model) {
        if (!available()) {
            return List.of();
        }
        try {
            if (!(rawParts(model) instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<Object> parts = new ArrayList<>();
            for (Object part : iterable) {
                if (boxClass().isInstance(part)) {
                    parts.add(part);
                }
            }
            return parts;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            fail("part collection", model, model, failure);
            return List.of();
        }
    }

    /** The parent box of {@code model}'s {@code part}, or {@code null} for a root or on reflection failure. */
    private Object parentOf(EntityModel<?> model, Object part) {
        try {
            Object parent = rawParent(part);
            return boxClass().isInstance(parent) ? parent : null;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            fail("parent lookup", model, part, failure);
            return null;
        }
    }

    /** Apply the local pose transform of {@code model}'s {@code part}; false on reflection failure. */
    private boolean applyTransform(EntityModel<?> model, Object part, PoseStack poseStack) {
        try {
            rawTransform(part, poseStack);
            return true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            fail("pose transform", model, part, failure);
            return false;
        }
    }

    private ModelIndex buildIndex(EntityModel<?> model) {
        List<Object> parts = collectParts(model);
        if (parts.isEmpty()) {
            return ModelIndex.EMPTY;
        }
        Map<Object, String> fallbackNames = fallbackNames(model, parts);
        List<String> paths = new ArrayList<>(parts.size());
        for (Object part : parts) {
            paths.add(pathOf(model, part, fallbackNames));
            if (failed(model)) {
                return ModelIndex.EMPTY;
            }
        }
        return new ModelIndex(List.copyOf(parts), List.copyOf(paths));
    }

    /**
     * Segment names for boxes whose {@link #intrinsicName} is empty: the model's Java field name
     * holding the box where one exists (guarded reflection by type),
     * else the box's positional {@code #N}. An unlisted, unnamed, field-less box (a stray ancestor)
     * has no entry and is skipped when it appears mid-path.
     */
    private Map<Object, String> fallbackNames(EntityModel<?> model, List<Object> parts) {
        Map<Object, String> names = new IdentityHashMap<>();
        for (int i = 0; i < parts.size(); i++) {
            names.put(parts.get(i), EyeConfigModel.attachmentIndex(i));
        }
        for (Class<?> c = model.getClass(); c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!boxClass().isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object box = field.get(model);
                    if (boxClass().isInstance(box)) {
                        names.put(box, field.getName());
                    }
                } catch (ReflectiveOperationException | RuntimeException accessDenied) {
                    ClientIntegrationFailures.warnOnce(familyLabel(),
                            "fallback field access", model.getClass().getName(), accessDenied);
                    // Positional names remain a reliable fallback when a Java field is inaccessible.
                }
            }
        }
        return names;
    }

    /**
     * The index of the box {@code token} addresses, or {@code -1}: the first box — in all-boxes order —
     * whose path suffix-matches. A nameless box's {@code #N} segment matches here like any other segment.
     */
    private static int find(ModelIndex index, String token) {
        for (int i = 0; i < index.paths().size(); i++) {
            if (EyeAttachmentResolver.pathMatches(token, index.paths().get(i))) {
                return i;
            }
        }
        return -1;
    }

    private ModelIndex indexOf(EntityModel<?> model) {
        return cache.computeIfAbsent(model, this::buildIndex);
    }

    /**
     * Root-first slash path for {@code part}: each ancestor's intrinsic name when set, else its
     * fallback (field name / {@code #N}); ancestors with neither are skipped. The part itself is
     * always listed, so its own segment — and hence the path — is never empty.
     */
    private String pathOf(EntityModel<?> model, Object part, Map<Object, String> fallbackNames) {
        ArrayDeque<String> segments = new ArrayDeque<>();
        for (Object box = part; box != null; box = parentOf(model, box)) {
            String segment = segmentName(box, fallbackNames);
            if (segment != null) {
                segments.addFirst(segment);
            }
        }
        return EyeConfigModel.attachmentPath(segments);
    }

    private String segmentName(Object box, Map<Object, String> fallbackNames) {
        String named = intrinsicName(box);
        if (!EyeAttachmentResolver.normalize(named).isEmpty()) {
            return named;
        }
        return fallbackNames.get(box);
    }

    @Override
    public String canonicalToken(EntityModel<?> model, String storedToken) {
        if (!handles(model)) {
            return storedToken;
        }
        ModelIndex index = indexOf(model);
        int i = find(index, storedToken);
        return i < 0 ? storedToken : index.paths().get(i);
    }

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        if (!handles(model)) {
            return List.of();
        }
        // Index-aligned with the all-boxes list, so a token's list position is the box's #N position.
        return indexOf(model).paths();
    }

    /**
     * The boxes from a root down to the attach box, inclusive, in the order they transform. Bound to the
     * resolver that produced it, since only the family knows how to apply a box's transform.
     */
    private final class BoxChain implements Attachment {
        private final Object[] chain;
        private final EntityModel<?> model;

        BoxChain(EntityModel<?> model, Object[] chain) {
            this.model = model;
            this.chain = chain;
        }

        @Override
        public boolean apply(PoseStack poseStack) {
            if (failed(model)) {
                return false;
            }
            for (Object part : chain) {
                if (!applyTransform(model, part, poseStack)) {
                    return false;
                }
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
        ModelIndex index = indexOf(model);
        int i = find(index, partToken);
        if (i < 0) {
            return null;
        }

        ArrayDeque<Object> chain = new ArrayDeque<>();
        for (Object part = index.parts().get(i); part != null; part = parentOf(model, part)) {
            chain.addFirst(part);
        }
        if (failed(model)) {
            return null;
        }
        return new BoxChain(model, chain.toArray());
    }
}
