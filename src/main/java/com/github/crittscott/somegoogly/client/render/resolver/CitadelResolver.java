package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolver for Citadel / LLibrary-style models used by mods such as Alex's Mobs and Ice and Fire.
 *
 * <p>Citadel is an optional dependency, so this resolver uses reflection only. It targets
 * {@code AdvancedEntityModel#getAllParts()} and {@code AdvancedModelBox}, whose boxes expose a
 * stable author-facing {@code boxName}, parent pointers, and {@code translateAndRotate(PoseStack)}.
 */
public class CitadelResolver implements EyeAttachmentResolver {

    private static final String ADVANCED_ENTITY_MODEL = "com.github.alexthe666.citadel.client.model.AdvancedEntityModel";
    private static final String ADVANCED_MODEL_BOX = "com.github.alexthe666.citadel.client.model.AdvancedModelBox";
    private static final Map<EntityModel<?>, List<Object>> CACHE = new WeakHashMap<>();
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

    private static String boxName(Object part) {
        try {
            Object value = HANDLES.boxName.get(part);
            return value instanceof String s ? s : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static Object byIndex(List<Object> parts, String token) {
        try {
            int index = Integer.parseInt(token.substring(1));
            return index >= 0 && index < parts.size() ? parts.get(index) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Object byName(List<Object> parts, String token) {
        String wanted = EyeAttachmentResolver.normalize(token);
        if (wanted.isEmpty()) {
            return null;
        }
        for (Object part : parts) {
            if (wanted.equals(normalizedName(part))) {
                return part;
            }
        }
        return null;
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

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        if (!handles(model)) {
            return List.of();
        }

        List<Object> parts = partsOf(model);
        Map<String, Integer> nameCounts = new HashMap<>();
        for (Object part : parts) {
            String name = normalizedName(part);
            if (!name.isEmpty()) {
                nameCounts.merge(name, 1, Integer::sum);
            }
        }

        List<String> tokens = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            Object part = parts.get(i);
            String displayName = boxName(part);
            String normalized = EyeAttachmentResolver.normalize(displayName);
            tokens.add(!normalized.isEmpty() && nameCounts.getOrDefault(normalized, 0) == 1 ? displayName : "#" + i);
        }
        return tokens;
    }

    @Override
    public boolean handles(EntityModel<?> model) {
        return HANDLES.available() && HANDLES.modelClass.isInstance(model);
    }

    private static String normalizedName(Object part) {
        return EyeAttachmentResolver.normalize(boxName(part));
    }

    private static Object parentOf(Object part) {
        try {
            Object parent = HANDLES.getParent.invoke(part);
            return HANDLES.boxClass.isInstance(parent) ? parent : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Object> partsOf(EntityModel<?> model) {
        return CACHE.computeIfAbsent(model, CitadelResolver::collectParts);
    }

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        if (!handles(model)) {
            return false;
        }

        List<Object> parts = partsOf(model);
        Object target = partToken != null && partToken.startsWith("#")
                ? byIndex(parts, partToken)
                : byName(parts, partToken);
        if (target == null) {
            return false;
        }

        ArrayDeque<Object> chain = new ArrayDeque<>();
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        for (Object part = target; part != null; part = parentOf(part)) {
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

    private static boolean translateAndRotate(Object part, PoseStack poseStack) {
        try {
            HANDLES.translateAndRotate.invoke(part, poseStack);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
