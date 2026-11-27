package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.client.render.EyeRenderData;
import com.github.crittscott.somegoogly.client.render.LayerGooglyEyes;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Optional bridge to Ice and Fire Community Edition's mob renderers. Its mobs draw Uranus models,
 * which are not vanilla {@code EntityModel}s, from {@code LegacyMobRenderer}, a plain {@code
 * EntityRenderer} rather than a {@code LivingEntityRenderer}, so neither the vanilla eye layer nor
 * the living render-state hook reaches them. Instead each such renderer is given one of its own
 * {@code LegacyEntityFeature}s through its public {@code addLayer}, built as a reflective proxy so
 * there is no compile-time dependency.
 *
 * <p>The renderer runs its features inside the same base model pose {@code LivingEntityRenderer}
 * gives its layers, right after posing the model's shared boxes for this entity and before another
 * entity of the type can re-pose them, so attachments resolve against this entity's pose. The
 * renderer extracts no living render state, so the eye decision is taken from the live entity the
 * feature receives, at submit time, where the renderer itself reads it.
 *
 * <p>The cockatrice's renderer is built directly on {@code EntityRenderer} and has no features;
 * {@code IceAndFireModelRendererMixin} calls {@link #submitAfterModel} from inside its {@code
 * submit} instead. It draws one of two static models, chosen by age.
 */
public final class IceAndFireCompat {

    private static final String LABEL = "Ice and Fire CE";
    private static final String PACKAGE = "com.iafenvoy.iceandfire.render.entity.";
    private static final Handles HANDLES = Handles.load();

    private IceAndFireCompat() {}

    private record Handles(
            @Nullable Class<?> rendererClass,
            @Nullable Class<?> featureClass,
            @Nullable Method addLayer,
            @Nullable Method getLegacyModel,
            @Nullable Field stateEntity,
            @Nullable Class<?> cockatriceClass,
            @Nullable Field cockatriceAdult,
            @Nullable Field cockatriceBaby) {
        static final Handles ABSENT = new Handles(null, null, null, null, null, null, null, null);

        boolean available() {
            return rendererClass != null;
        }

        static Handles load() {
            String rendererName = PACKAGE + "LegacyMobRenderer";
            try {
                Class<?> renderer = Class.forName(rendererName);
                Class<?> feature = Class.forName(PACKAGE + "LegacyEntityFeature");
                Field stateEntity =
                        Class.forName(PACKAGE + "LegacyEntityRenderState").getField("entity");
                Method addLayer = renderer.getMethod("addLayer", feature);
                Method getLegacyModel = renderer.getMethod("getLegacyModel");
                // The cockatrice's models are its own; losing them must not cost the other mobs
                // their eyes.
                String cockatriceName = PACKAGE + "CockatriceEntityRenderer";
                try {
                    Class<?> cockatrice = Class.forName(cockatriceName);
                    return new Handles(
                            renderer,
                            feature,
                            addLayer,
                            getLegacyModel,
                            stateEntity,
                            cockatrice,
                            cockatrice.getField("ADULT_MODEL"),
                            cockatrice.getField("BABY_MODEL"));
                } catch (ReflectiveOperationException | LinkageError failure) {
                    ClientIntegrationFailures.warnOnce(
                            LABEL, "API discovery", cockatriceName, failure);
                    return new Handles(
                            renderer,
                            feature,
                            addLayer,
                            getLegacyModel,
                            stateEntity,
                            null,
                            null,
                            null);
                }
            } catch (ClassNotFoundException absent) {
                return ABSENT;
            } catch (ReflectiveOperationException | LinkageError failure) {
                ClientIntegrationFailures.warnOnce(LABEL, "API discovery", rendererName, failure);
                return ABSENT;
            }
        }
    }

    /**
     * Give an Ice and Fire CE legacy mob renderer the eye feature; any other renderer is ignored.
     *
     * @return whether the feature was added
     */
    public static boolean install(EntityRenderer<?, ?> renderer) {
        if (!HANDLES.available() || !HANDLES.rendererClass().isInstance(renderer)) {
            return false;
        }
        Class<?> featureClass = HANDLES.featureClass();
        Object feature =
                Proxy.newProxyInstance(
                        featureClass.getClassLoader(),
                        new Class<?>[] {featureClass},
                        (proxy, method, args) -> invokeFeature(renderer, proxy, method, args));
        try {
            HANDLES.addLayer().invoke(renderer, feature);
            return true;
        } catch (ReflectiveOperationException failure) {
            ClientIntegrationFailures.warnOnce(
                    LABEL, "feature installation", renderer.getClass().getName(), failure);
            return false;
        }
    }

    /**
     * The proxy's dispatch. {@code LegacyEntityFeature}'s only abstract method is {@code
     * submit(entity, partialTick, poseStack, collector, camera, lightCoords, outlineColor)}; the
     * rest are {@code Object}'s, answered by identity.
     */
    @Nullable
    private static Object invokeFeature(
            EntityRenderer<?, ?> renderer, Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> "Googly-eye feature for " + renderer.getClass().getName();
            };
        }
        if (args[0] instanceof LivingEntity living) {
            submit(
                    renderer,
                    living,
                    (Float) args[1],
                    (PoseStack) args[2],
                    (SubmitNodeCollector) args[3],
                    (Integer) args[5]);
        }
        return null;
    }

    /** Whether Ice and Fire CE's legacy renderer API was found and bound. */
    public static boolean isAvailable() {
        return HANDLES.available();
    }

    /**
     * The Uranus model {@code renderer} draws {@code living} with, or {@code null} when it is not
     * an Ice and Fire CE legacy mob renderer or the cockatrice's, its model is not loaded yet, or
     * the lookup failed.
     */
    @Nullable
    public static Object model(EntityRenderer<?, ?> renderer, LivingEntity living) {
        if (!HANDLES.available()) {
            return null;
        }
        try {
            if (HANDLES.rendererClass().isInstance(renderer)) {
                return HANDLES.getLegacyModel().invoke(renderer);
            }
            if (HANDLES.cockatriceClass() != null
                    && HANDLES.cockatriceClass().isInstance(renderer)) {
                return (living.isBaby() ? HANDLES.cockatriceBaby() : HANDLES.cockatriceAdult())
                        .get(null);
            }
            return null;
        } catch (ReflectiveOperationException failure) {
            ClientIntegrationFailures.warnOnce(
                    LABEL, "model lookup", renderer.getClass().getName(), failure);
            return null;
        }
    }

    private static void submit(
            EntityRenderer<?, ?> renderer,
            LivingEntity living,
            float partialTick,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight) {
        EyeRenderData data = EyeRenderData.extract(living, partialTick);
        if (!data.draws()) {
            return;
        }
        Object model = model(renderer, living);
        if (model == null) {
            return;
        }
        // The red hurt tint Ice and Fire's legacy renderer gives the model itself.
        int overlay =
                OverlayTexture.pack(
                        OverlayTexture.u(0.0F),
                        OverlayTexture.v(living.hurtTime > 0 || living.deathTime > 0));
        LayerGooglyEyes.submitEyes(
                poseStack, collector, packedLight, overlay, data, model, living.isBaby(), 0.0F);
    }

    /**
     * Draw eyes for an Ice and Fire CE renderer that submits its model itself. Called right after
     * that submission, while {@code poseStack} still holds the model pose and the shared model is
     * posed for this entity.
     */
    public static void submitAfterModel(
            EntityRenderer<?, ?> renderer,
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (!HANDLES.available()) {
            return;
        }
        Object entity;
        try {
            entity = HANDLES.stateEntity().get(state);
        } catch (ReflectiveOperationException failure) {
            ClientIntegrationFailures.warnOnce(
                    LABEL, "render-state entity", renderer.getClass().getName(), failure);
            return;
        }
        if (entity instanceof LivingEntity living) {
            // The render state's partialTick field is a NeoForge addition, absent from the vanilla
            // state common sees.
            submit(
                    renderer,
                    living,
                    Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false),
                    poseStack,
                    collector,
                    state.lightCoords);
        }
    }
}
