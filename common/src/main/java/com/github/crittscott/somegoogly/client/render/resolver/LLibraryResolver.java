package com.github.crittscott.somegoogly.client.render.resolver;

import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        static final Handles ABSENT = new Handles(null, null, null, null, null, null);

        boolean available() {
            return modelClass != null;
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
            } catch (ClassNotFoundException absent) {
                return ABSENT;
            } catch (ReflectiveOperationException | LinkageError failure) {
                ClientIntegrationFailures.warnOnce(
                        "LLibraryResolver", "API discovery", BASIC_MODEL_BASE, failure);
                return ABSENT;
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
        return available() && HANDLES.modelClass().isInstance(model) && !failed(model);
    }

    @Override
    protected Object rawParts(EntityModel<?> model) throws ReflectiveOperationException {
        return HANDLES.boxList().get(model);
    }

    @Override
    protected String intrinsicName(Object part) {
        return ""; // the family has no box name field; field names / #N carry the vocabulary
    }

    @Override
    protected Object rawParent(Object part) throws ReflectiveOperationException {
        return HANDLES.advancedBoxClass().isInstance(part) ? HANDLES.getParent().invoke(part) : null;
    }

    @Override
    protected void rawTransform(Object part, PoseStack poseStack) throws ReflectiveOperationException {
        HANDLES.translateRotate().invoke(part, poseStack);
    }
}
