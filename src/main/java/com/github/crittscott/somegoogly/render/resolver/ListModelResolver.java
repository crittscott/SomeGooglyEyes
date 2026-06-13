package com.github.crittscott.somegoogly.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Resolver for the {@link AgeableListModel} / {@link ListModel} families (humanoids, quadrupeds,
 * etc.). These models do <b>not</b> retain a named root, and their part groups
 * ({@code headParts()}/{@code bodyParts()}/{@code parts()}) are {@code protected}.
 *
 * <p>We reach the parts by reflecting the model's {@link ModelPart}-typed fields, addressing by
 * <b>type + declaration order</b>, never by name. Obfuscation renames fields but preserves their
 * order, so this is production-safe (unlike the original name-based reflection). Fields are gathered
 * <b>superclass-first</b>, so the head — declared first in the vanilla bases ({@code HumanoidModel},
 * {@code QuadrupedModel}) — is element 0.
 *
 * <p>An earlier M1 attempt used an {@code @Invoker} mixin here; it required a MixinGradle refmap that
 * didn't end up in the jar, and an unmatched {@code @Invoker} fails <i>fatally at class-transform
 * time</i> (not at call time), crashing the whole game when any mod loaded {@code AgeableListModel}.
 * Reflection has no such failure mode: every access is guarded, so the worst case is "no eyes for
 * this mob", never a crash.
 *
 * <p>Limitation: addressing is positional (head = first {@code ModelPart} field), so a non-head
 * token, or a mod model that declares extra parts above the head, may resolve imperfectly. That is
 * the per-mob visual-authoring concern the M2 picker is meant to handle.
 */
public class ListModelResolver implements EyeAttachmentResolver {

    // Resolved part list per model instance (models are singletons per renderer; render thread only).
    private static final Map<EntityModel<?>, List<ModelPart>> CACHE = new WeakHashMap<>();

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof AgeableListModel || model instanceof ListModel;
    }

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        ModelPart part = headPart(model);
        if (part == null) {
            return false;
        }
        // Top-level list parts have no animated ancestors, so a single translateAndRotate reproduces
        // their rendered pose.
        part.translateAndRotate(poseStack);
        return true;
    }

    private static ModelPart headPart(EntityModel<?> model) {
        List<ModelPart> parts = CACHE.computeIfAbsent(model, ListModelResolver::collectModelPartFields);
        return parts.isEmpty() ? null : parts.get(0);
    }

    private static List<ModelPart> collectModelPartFields(EntityModel<?> model) {
        List<ModelPart> result = new ArrayList<>();
        collect(model, model.getClass(), result);
        return result;
    }

    private static void collect(EntityModel<?> model, Class<?> cls, List<ModelPart> out) {
        if (cls == null || cls == Object.class) {
            return;
        }
        collect(model, cls.getSuperclass(), out); // superclass-first ordering
        try {
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType() == ModelPart.class) {
                    f.setAccessible(true);
                    Object value = f.get(model);
                    if (value instanceof ModelPart mp) {
                        out.add(mp);
                    }
                }
            }
        } catch (Throwable accessDenied) {
            // Module/access restriction or anything else: degrade to "no eyes" for this model.
        }
    }
}
