package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolver for LLibrary-style models. Mowzie's Mobs ships its own fork of LLibrary's model toolkit
 * shaded at {@code com.ilexiconn.llibrary}, and its pre-GeckoLib mobs (Ferrous Wroughtnaut, Frostmaw,
 * Naga, Foliaath, Grottol, Lantern) are vanilla {@code MobRenderer}s over these models — no vanilla
 * {@code ModelPart}s for the reflection fallback and no {@code GeoEntityRenderer} for the GeckoLib
 * layer, so without this family they are invisible to the picker.
 *
 * <p>Boxes register themselves in the model's public {@code boxList} at construction, giving the
 * all-boxes enumeration. They carry no name field, so segments come from the model's Java field
 * names (Mowzie's models hold every part in a named field), with positional {@code #N} as the last
 * resort — see {@link ReflectedBoxResolver} for the shared vocabulary. Parent pointers exist only on
 * {@code AdvancedModelRenderer} (set by {@code addChild}); a plain {@code BasicModelRenderer}
 * mid-tree would truncate the chain, but Mowzie's models are Advanced throughout.
 * {@code translateRotate} performs the family's parent-scale compensation itself, so a root→box
 * chain of calls reproduces the render-time transform.
 */
public class LLibraryResolver extends ReflectedBoxResolver {

    private static final String BASIC_MODEL_BASE = "com.ilexiconn.llibrary.client.model.tools.BasicModelBase";
    private static final String BASIC_MODEL_RENDERER = "com.ilexiconn.llibrary.client.model.tools.BasicModelRenderer";
    private static final String ADVANCED_MODEL_RENDERER = "com.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer";
    private static final Handles HANDLES = Handles.load();

    private record Handles(Class<?> modelClass, Class<?> boxClass, Class<?> advancedBoxClass,
                           Field boxList, Method getParent, Method translateRotate) {
        boolean available() {
            return modelClass != null && boxClass != null && advancedBoxClass != null
                    && boxList != null && getParent != null && translateRotate != null;
        }

        static Handles load() {
            try {
                Class<?> modelClass = Class.forName(BASIC_MODEL_BASE);
                Class<?> boxClass = Class.forName(BASIC_MODEL_RENDERER);
                Class<?> advancedBoxClass = Class.forName(ADVANCED_MODEL_RENDERER);
                Field boxList = modelClass.getField("boxList");
                Method getParent = advancedBoxClass.getMethod("getParent");
                Method translateRotate = boxClass.getMethod("translateRotate", PoseStack.class);
                return new Handles(modelClass, boxClass, advancedBoxClass, boxList, getParent, translateRotate);
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
            Object raw = HANDLES.boxList().get(model);
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
        return ""; // the family has no box name field; field names / #N carry the vocabulary
    }

    @Override
    protected Object parentOf(Object part) {
        if (!HANDLES.advancedBoxClass().isInstance(part)) {
            return null;
        }
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
            HANDLES.translateRotate().invoke(part, poseStack);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
