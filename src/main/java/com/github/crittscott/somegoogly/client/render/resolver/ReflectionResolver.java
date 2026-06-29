package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Catch-all resolver for any vanilla {@link EntityModel} that isn't a {@link net.minecraft.client.model.HierarchicalModel}
 * (humanoids, quadrupeds, and other list/direct models). These models retain no named root, so parts
 * are reached by reflecting {@link ModelPart} fields and addressed by <b>type + declaration order</b>
 * — never by name (which obfuscation renames). Fields are gathered <b>superclass-first</b>, so the
 * head — declared first in the vanilla bases ({@code HumanoidModel}, {@code QuadrupedModel}) — is
 * index 0.
 *
 * <p>Tokens are {@code #N} (the field index) only. A name token can't be resolved by this model family
 * (there are no obfuscation-stable names to match), so it fails rather than being remapped.
 *
 * <p>Registered after {@link HierarchicalResolver} so named models keep the better named path; this
 * one is the fallback for everything else. Every access is guarded, so an unreflectable model just
 * yields no eyes — never a crash.
 *
 * <p>Known limits: indices shift if a mod renames/reorders/adds parts in an update; and
 * {@code AgeableListModel}'s baby scale/offset happens outside the part tree, so baby mobs may be
 * mis-placed.
 */
public class ReflectionResolver implements EyeAttachmentResolver {

    // Resolved part list per model instance (models are singletons per renderer; render thread only).
    private static final Map<EntityModel<?>, List<ModelPart>> CACHE = new WeakHashMap<>();

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

    private static List<ModelPart> collectModelPartFields(EntityModel<?> model) {
        List<ModelPart> result = new ArrayList<>();
        collect(model, model.getClass(), result);
        return result;
    }

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        int count = partsOf(model).size();
        List<String> tokens = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tokens.add("#" + i);
        }
        return tokens;
    }

    @Override
    public String canonicalToken(EntityModel<?> model, String storedToken) {
        int index = indexFor(storedToken);
        if (index < 0 || index >= partsOf(model).size()) {
            return storedToken; // can't place it; leave the token as authored
        }
        return "#" + index;
    }

    @Override
    public boolean handles(EntityModel<?> model) {
        return true; // catch-all fallback (HierarchicalResolver is tried first)
    }

    private static int indexFor(String token) {
        if (token != null && token.startsWith("#")) {
            try {
                return Integer.parseInt(token.substring(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        // Index-only models address parts by #N exclusively. A name token can't be resolved here (there
        // are no production-stable names to match against), so it fails rather than guessing the head.
        return -1;
    }

    private static List<ModelPart> partsOf(EntityModel<?> model) {
        return CACHE.computeIfAbsent(model, ReflectionResolver::collectModelPartFields);
    }

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        List<ModelPart> parts = partsOf(model);
        int index = indexFor(partToken);
        if (index < 0 || index >= parts.size()) {
            return false;
        }
        // Top-level parts have no animated ancestors, so a single translateAndRotate is the pose.
        parts.get(index).translateAndRotate(poseStack);
        return true;
    }
}
