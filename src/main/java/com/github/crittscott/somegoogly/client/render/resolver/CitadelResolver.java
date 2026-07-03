package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolver for Citadel / LLibrary-style models used by mods such as Alex's Mobs and Ice and Fire.
 *
 * <p>Citadel is an optional dependency, so this resolver uses reflection only. It targets
 * {@code AdvancedEntityModel#getAllParts()} and {@code AdvancedModelBox}, whose boxes expose an
 * author-facing {@code boxName}, parent pointers, and {@code translateAndRotate(PoseStack)}.
 *
 * <p>Tokens are the same full slash paths the vanilla resolvers speak — the box's parent chain,
 * suffix-matched via {@link EyeAttachmentResolver#pathMatches}, so a stored {@code Head} attaches to
 * {@code Tail_1/Body/Head} and a path disambiguates same-named boxes under different parents. Each
 * segment comes from three sources, in priority order:
 * <ol>
 *   <li>the box's {@code boxName} — set by tabula-loaded models (Ice and Fire's dragons) and by mods
 *       that use the naming constructor (Alex's Mobs);</li>
 *   <li>the name of the model's own Java field holding the box. Mod classes are never
 *       obfuscation-mangled (only Minecraft/Forge classes are), so field names are as stable as box
 *       names — and e.g. Ice and Fire builds every box in its hand-written models through the
 *       nameless texture-offset constructor, leaving the real names ({@code Head}, {@code Left_Arm},
 *       …) only on its fields;</li>
 *   <li>the positional {@code #N} (the box's index in {@code getAllParts()}). A stored pure
 *       {@code #N} token also always resolves by position, whatever the box's segment name is, so
 *       configs authored before names were recoverable keep working.</li>
 * </ol>
 */
public class CitadelResolver implements EyeAttachmentResolver {

    private static final String ADVANCED_ENTITY_MODEL = "com.github.alexthe666.citadel.client.model.AdvancedEntityModel";
    private static final String ADVANCED_MODEL_BOX = "com.github.alexthe666.citadel.client.model.AdvancedModelBox";
    private static final Map<EntityModel<?>, ModelIndex> CACHE = new WeakHashMap<>();
    private static final Handles HANDLES = Handles.load();

    private record Handles(Class<?> modelClass, Class<?> boxClass, Method getAllParts,
                           Method getParent, Method translateAndRotate, Field boxName) {
        boolean available() {
            return modelClass != null && boxClass != null && getAllParts != null
                    && getParent != null && translateAndRotate != null && boxName != null;
        }

        static Handles load() {
            try {
                Class<?> modelClass = Class.forName(ADVANCED_ENTITY_MODEL);
                Class<?> boxClass = Class.forName(ADVANCED_MODEL_BOX);
                Method getAllParts = modelClass.getMethod("getAllParts");
                Method getParent = boxClass.getMethod("getParent");
                Method translateAndRotate = boxClass.getMethod("translateAndRotate", PoseStack.class);
                Field boxName = boxClass.getField("boxName");
                return new Handles(modelClass, boxClass, getAllParts, getParent, translateAndRotate, boxName);
            } catch (Throwable ignored) {
                return new Handles(null, null, null, null,
                        null, null);
            }
        }
    }

    /** Per-model index: the boxes from {@code getAllParts()}, with an index-aligned path token each. */
    private record ModelIndex(List<Object> parts, List<String> paths) {
        static final ModelIndex EMPTY = new ModelIndex(List.of(), List.of());
    }

    private static String boxName(Object part) {
        try {
            Object value = HANDLES.boxName.get(part);
            return value instanceof String s ? s : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static ModelIndex buildIndex(EntityModel<?> model) {
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

    private static List<Object> collectParts(EntityModel<?> model) {
        if (!HANDLES.available()) {
            return List.of();
        }
        try {
            Object raw = HANDLES.getAllParts.invoke(model);
            if (!(raw instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<Object> parts = new ArrayList<>();
            for (Object part : iterable) {
                if (HANDLES.boxClass.isInstance(part)) {
                    parts.add(part);
                }
            }
            return parts;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    /**
     * Segment names for boxes whose {@code boxName} is empty: the model's Java field name holding the
     * box where one exists (guarded reflection by type, like {@code ChildMapResolver}), else the box's
     * positional {@code #N}. An unlisted, unnamed, field-less box (a stray ancestor) has no entry and
     * is skipped when it appears mid-path.
     */
    private static Map<Object, String> fallbackNames(EntityModel<?> model, List<Object> parts) {
        Map<Object, String> names = new IdentityHashMap<>();
        for (int i = 0; i < parts.size(); i++) {
            names.put(parts.get(i), "#" + i);
        }
        for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!HANDLES.boxClass.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object box = field.get(model);
                    if (HANDLES.boxClass.isInstance(box)) {
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
     * The index of the box {@code token} addresses, or {@code -1}: a pure {@code #N} resolves by
     * position (kept so pre-path configs and nameless boxes stay addressable), anything else is the
     * first box — in {@code getAllParts()} order — whose path suffix-matches.
     */
    private static int find(ModelIndex index, String token) {
        if (token == null) {
            return -1;
        }
        if (token.startsWith("#") && token.indexOf('/') < 0) {
            int position = parseIndex(index, token);
            if (position >= 0) {
                return position;
            }
        }
        for (int i = 0; i < index.paths().size(); i++) {
            if (EyeAttachmentResolver.pathMatches(token, index.paths().get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static ModelIndex indexOf(EntityModel<?> model) {
        // Indexed per model instance (models are singletons per renderer; render thread only).
        return CACHE.computeIfAbsent(model, CitadelResolver::buildIndex);
    }

    private static int parseIndex(ModelIndex index, String token) {
        try {
            int i = Integer.parseInt(token.substring(1));
            return i >= 0 && i < index.parts().size() ? i : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Root-first slash path for {@code part}: each ancestor's {@code boxName} when set, else its
     * fallback (field name / {@code #N}); ancestors with neither are skipped. The part itself is
     * always listed, so its own segment — and hence the path — is never empty.
     */
    private static String pathOf(Object part, Map<Object, String> fallbackNames) {
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

    private static Object parentOf(Object part) {
        try {
            Object parent = HANDLES.getParent.invoke(part);
            return HANDLES.boxClass.isInstance(parent) ? parent : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String segmentName(Object box, Map<Object, String> fallbackNames) {
        String named = boxName(box);
        if (!EyeAttachmentResolver.normalize(named).isEmpty()) {
            return named;
        }
        return fallbackNames.get(box);
    }

    private static boolean translateAndRotate(Object part, PoseStack poseStack) {
        try {
            HANDLES.translateAndRotate.invoke(part, poseStack);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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
        // Index-aligned with getAllParts(), so a token's list position is the box's #N position.
        return indexOf(model).paths();
    }

    @Override
    public boolean handles(EntityModel<?> model) {
        return HANDLES.available() && HANDLES.modelClass.isInstance(model);
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
            if (!translateAndRotate(part, poseStack)) {
                return false;
            }
        }
        return true;
    }
}
