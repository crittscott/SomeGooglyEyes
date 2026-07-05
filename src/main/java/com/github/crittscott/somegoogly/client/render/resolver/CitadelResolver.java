package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolver for Citadel models used by mods such as Alex's Mobs and Ice and Fire.
 *
 * <p>Citadel is an optional dependency, so this resolver uses reflection only. It targets
 * {@code AdvancedEntityModel#getAllParts()} and {@code AdvancedModelBox}, whose boxes expose an
 * author-facing {@code boxName}, parent pointers, and {@code translateAndRotate(PoseStack)}.
 *
 * <p>Token vocabulary and matching live in {@link ReflectedBoxResolver}. The intrinsic segment name
 * here is the box's {@code boxName} — set by tabula-loaded models (Ice and Fire's dragons) and by
 * mods that use the naming constructor (Alex's Mobs); e.g. Ice and Fire builds every box in its
 * hand-written models through the nameless texture-offset constructor, leaving the real names
 * ({@code Head}, {@code Left_Arm}, …) only on its fields, which the shared field-name fallback
 * recovers.
 */
public class CitadelResolver extends ReflectedBoxResolver {

    private static final String ADVANCED_ENTITY_MODEL = "com.github.alexthe666.citadel.client.model.AdvancedEntityModel";
    private static final String ADVANCED_MODEL_BOX = "com.github.alexthe666.citadel.client.model.AdvancedModelBox";
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

    @Override
    protected boolean available() {
        return HANDLES.available();
    }

    @Override
    protected Class<?> boxClass() {
        return HANDLES.boxClass();
    }

    @Override
    public boolean handles(EntityModel<?> model) {
        return HANDLES.available() && HANDLES.modelClass().isInstance(model);
    }

    @Override
    protected List<Object> collectParts(EntityModel<?> model) {
        if (!HANDLES.available()) {
            return List.of();
        }
        try {
            Object raw = HANDLES.getAllParts().invoke(model);
            if (!(raw instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<Object> parts = new ArrayList<>();
            for (Object part : iterable) {
                if (HANDLES.boxClass().isInstance(part)) {
                    parts.add(part);
                }
            }
            return parts;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    @Override
    protected String intrinsicName(Object part) {
        try {
            Object value = HANDLES.boxName().get(part);
            return value instanceof String s ? s : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    @Override
    protected Object parentOf(Object part) {
        try {
            Object parent = HANDLES.getParent().invoke(part);
            return HANDLES.boxClass().isInstance(parent) ? parent : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    protected boolean applyTransform(Object part, PoseStack poseStack) {
        try {
            HANDLES.translateAndRotate().invoke(part, poseStack);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
