package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared machinery for resolvers over reflected "box-tree" model families — external mods whose
 * models keep their own box hierarchy (a flat all-boxes list plus parent pointers and a per-box pose
 * transform) instead of vanilla {@code ModelPart}s. {@link CitadelResolver} and
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

    // Indexed per model instance (models are singletons per renderer; render thread only).
    private final Map<EntityModel<?>, ModelIndex> cache = new WeakHashMap<>();

    /** Per-model index: the family's boxes, with an index-aligned path token each. */
    private record ModelIndex(List<Object> parts, List<String> paths) {
        static final ModelIndex EMPTY = new ModelIndex(List.of(), List.of());
    }

    /** Whether the reflective handles all loaded (the family's classes are present and API-shaped as expected). */
    protected abstract boolean available();

    /** The family's box class, for instance checks and the field-name fallback scan. */
    protected abstract Class<?> boxClass();

    /** All of the model's boxes in the family's stable order; empty on any reflection failure. */
    protected abstract List<Object> collectParts(EntityModel<?> model);

    /** The box's author-set name, or {@code ""} when the family has none / it is unset. */
    protected abstract String intrinsicName(Object part);

    /** The box's parent box, or {@code null} for a root (or on reflection failure). */
    protected abstract Object parentOf(Object part);

    /** Apply the box's local pose transform (the family's translate+rotate). False on reflection failure. */
    protected abstract boolean applyTransform(Object part, PoseStack poseStack);

    private ModelIndex buildIndex(EntityModel<?> model) {
        List<Object> parts = collectParts(model);
        if (parts.isEmpty()) {
            return ModelIndex.EMPTY;
        }
        Map<Object, String> fallbackNames = fallbackNames(model, parts);
        List<String> paths = new ArrayList<>(parts.size());
        for (Object part : parts) {
            paths.add(pathOf(part, fallbackNames));
        }
        return new ModelIndex(List.copyOf(parts), List.copyOf(paths));
    }

    /**
     * Segment names for boxes whose {@link #intrinsicName} is empty: the model's Java field name
     * holding the box where one exists (guarded reflection by type, like {@code ChildMapResolver}),
     * else the box's positional {@code #N}. An unlisted, unnamed, field-less box (a stray ancestor)
     * has no entry and is skipped when it appears mid-path.
     */
    private Map<Object, String> fallbackNames(EntityModel<?> model, List<Object> parts) {
        Map<Object, String> names = new IdentityHashMap<>();
        for (int i = 0; i < parts.size(); i++) {
            names.put(parts.get(i), "#" + i);
        }
        for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
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
                } catch (Throwable accessDenied) {
                    // Module/access restriction or anything else: skip this field.
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
        if (token == null) {
            return -1;
        }
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
    private String pathOf(Object part, Map<Object, String> fallbackNames) {
        ArrayDeque<String> segments = new ArrayDeque<>();
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        for (Object box = part; box != null; box = parentOf(box)) {
            if (seen.put(box, Boolean.TRUE) != null) {
                break; // parent cycle: keep the segments gathered so far
            }
            String segment = segmentName(box, fallbackNames);
            if (segment != null) {
                segments.addFirst(segment);
            }
        }
        return String.join("/", segments);
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

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        if (!handles(model)) {
            return false;
        }
        ModelIndex index = indexOf(model);
        int i = find(index, partToken);
        if (i < 0) {
            return false;
        }

        ArrayDeque<Object> chain = new ArrayDeque<>();
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        for (Object part = index.parts().get(i); part != null; part = parentOf(part)) {
            if (seen.put(part, Boolean.TRUE) != null) {
                return false;
            }
            chain.addFirst(part);
        }

        for (Object part : chain) {
            if (!applyTransform(part, poseStack)) {
                return false;
            }
        }
        return true;
    }
}
