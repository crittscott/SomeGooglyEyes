package com.github.crittscott.somegoogly.client.render.resolver;

import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Resolver for the {@code AdvancedEntityModel} / {@code AdvancedModelBox} toolkit, shipped by two
 * libraries under different package names: Citadel (Alex's Mobs, Ice and Fire) and Uranus (Ice and
 * Fire Community Edition). Both expose the same API — an all-boxes list, parent pointers, an
 * author-facing {@code boxName}, and {@code translateAndRotate(PoseStack)} — so one instance per
 * library covers both, and they cannot drift apart.
 *
 * <p>Both libraries are optional dependencies, so this resolver uses reflection only. Each instance
 * owns its handles, its per-model cache, and its failure state: one library failing or being absent
 * never disables the other.
 *
 * <p>Token vocabulary and matching live in {@link ReflectedBoxResolver}. The intrinsic segment name
 * here is the box's {@code boxName} — set by tabula-loaded models (Ice and Fire's dragons) and by
 * mods that use the naming constructor (Alex's Mobs); e.g. Ice and Fire builds every box in its
 * hand-written models through the nameless texture-offset constructor, leaving the real names
 * ({@code Head}, {@code Left_Arm}, …) only on its fields, which the shared field-name fallback
 * recovers.
 */
public class AdvancedModelBoxResolver extends ReflectedBoxResolver {

    private static final String CITADEL_PACKAGE = "com.github.alexthe666.citadel.client.model.";
    private static final String URANUS_PACKAGE = "com.iafenvoy.uranus.client.model.";

    private final String familyLabel;
    private final Handles api;

    private AdvancedModelBoxResolver(String familyLabel, String modelClassName, String boxClassName) {
        this.familyLabel = familyLabel;
        this.api = Handles.load(familyLabel, modelClassName, boxClassName);
    }

    /** Citadel's copy of the toolkit: Alex's Mobs, Ice and Fire. */
    public static AdvancedModelBoxResolver citadel() {
        return new AdvancedModelBoxResolver("Citadel",
                CITADEL_PACKAGE + "AdvancedEntityModel", CITADEL_PACKAGE + "AdvancedModelBox");
    }

    /** Uranus' copy of the toolkit: Ice and Fire Community Edition. */
    public static AdvancedModelBoxResolver uranus() {
        return new AdvancedModelBoxResolver("Uranus",
                URANUS_PACKAGE + "AdvancedEntityModel", URANUS_PACKAGE + "AdvancedModelBox");
    }

    private record Handles(Class<?> modelClass, Class<?> boxClass, Method getAllParts,
                           Method getParent, Method translateAndRotate, Field boxName) {
        static final Handles ABSENT = new Handles(null, null, null, null, null, null);

        boolean available() {
            return modelClass != null;
        }

        static Handles load(String familyLabel, String modelClassName, String boxClassName) {
            try {
                Class<?> modelClass = Class.forName(modelClassName);
                Class<?> boxClass = Class.forName(boxClassName);
                Method getAllParts = modelClass.getMethod("getAllParts");
                Method getParent = boxClass.getMethod("getParent");
                Method translateAndRotate = boxClass.getMethod("translateAndRotate", PoseStack.class);
                Field boxName = boxClass.getField("boxName");
                return new Handles(modelClass, boxClass, getAllParts, getParent, translateAndRotate, boxName);
            } catch (ClassNotFoundException absent) {
                return ABSENT;
            } catch (ReflectiveOperationException | LinkageError failure) {
                ClientIntegrationFailures.warnOnce(
                        familyLabel, "API discovery", modelClassName, failure);
                return ABSENT;
            }
        }
    }

    @Override
    protected String familyLabel() {
        return familyLabel;
    }

    @Override
    protected boolean available() {
        return api.available();
    }

    @Override
    protected Class<?> boxClass() {
        return api.boxClass();
    }

    @Override
    public boolean handles(EntityModel<?> model) {
        return available() && api.modelClass().isInstance(model) && !failed(model);
    }

    @Override
    protected Object rawParts(EntityModel<?> model) throws ReflectiveOperationException {
        return api.getAllParts().invoke(model);
    }

    @Override
    protected String intrinsicName(Object part) {
        try {
            Object value = api.boxName().get(part);
            return value instanceof String s ? s : "";
        } catch (ReflectiveOperationException | RuntimeException failure) {
            ClientIntegrationFailures.warnOnce(
                    familyLabel, "box-name access", part.getClass().getName(), failure);
            return "";
        }
    }

    @Override
    protected Object rawParent(Object part) throws ReflectiveOperationException {
        return api.getParent().invoke(part);
    }

    @Override
    protected void rawTransform(Object part, PoseStack poseStack) throws ReflectiveOperationException {
        api.translateAndRotate().invoke(part, poseStack);
    }
}
