package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shared base for resolvers that walk a vanilla {@link ModelPart} tree by its name → child map. The three
 * concrete subclasses differ only in <i>where the roots come from</i> and what those roots are called:
 *
 * <ul>
 *   <li>{@link HierarchicalResolver} — the single public {@link net.minecraft.client.model.HierarchicalModel#root()},
 *       named {@code root}.</li>
 *   <li>{@link AgeableListResolver} — the {@code headParts()}/{@code bodyParts()} groups, named
 *       {@code head}/{@code body} (stable across obfuscation, unlike the SRG-mangled field names).</li>
 *   <li>{@link ChildMapResolver} — reflected top-level {@code ModelPart} fields, named positionally
 *       ({@code #0}, {@code #1}, …) since a root field has no obfuscation-stable name.</li>
 * </ul>
 *
 * <p>Reading {@link ModelPart}'s {@code children} map directly (made accessible by the mod's access
 * transformer) is what lets these recover the modder's real <b>descendant</b> names ({@code head1},
 * {@code body1}, …) on any model — not just hierarchical ones. Tokens are slash-joined paths
 * ({@code root/body/head}); matching is a normalized <b>suffix</b> match (see
 * {@link EyeAttachmentResolver#pathMatches}), so a stored {@code head} still attaches and a stored
 * {@code body/head} disambiguates two same-named parts under different parents.
 *
 * <p>The walk mirrors {@link ModelPart#visit}: it applies each part's {@code translateAndRotate} onto the
 * live (this-frame, post-animation) {@link PoseStack} before descending, so the captured pose includes
 * every ancestor's animation. Unlike {@code visit} it does <b>not</b> skip cube-less pivot/group parts, so
 * eyes can attach to a named empty joint (the limitation the old visit-based resolver documented).
 *
 * <p><b>Legacy {@code #N} compatibility.</b> Before this mod migrated to names, every non-hierarchical
 * model addressed parts by {@code #N} = the Nth {@link ModelPart} field in superclass-first declaration
 * order, attached with a single {@code translateAndRotate} (no ancestor walk). A {@code #N} token is still
 * resolved that way here — pixel-identical to the old behavior — so configs authored against {@code #N}
 * keep working until their data is migrated (see the {@code /sg migratetokens} dump). {@link #canonicalToken}
 * maps a {@code #N} token forward to the part's new path, which is how the migration computes the rewrite.
 */
abstract class ModelPartTreeResolver implements EyeAttachmentResolver {

    // Legacy #N -> field list (all ModelPart fields, superclass-first, UNFILTERED — matching the old
    // ReflectionResolver's index semantics). Distinct from ChildMapResolver's descendant-filtered roots.
    private static final Map<EntityModel<?>, List<ModelPart>> LEGACY_FIELDS = new WeakHashMap<>();

    /** A subtree root paired with the name it should appear under (roots have no intrinsic stable name). */
    protected record NamedRoot(String name, ModelPart part) {
    }

    /** The subtrees to walk for this model, in a stable order. Only called when {@link #handles} is true. */
    protected abstract List<NamedRoot> roots(EntityModel<?> model);

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        if (!handles(model)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (NamedRoot root : roots(model)) {
            enumerate("", root.name(), root.part(), out);
        }
        return out;
    }

    private static void enumerate(String prefix, String name, ModelPart part, List<String> out) {
        String path = prefix.isEmpty() ? name : prefix + "/" + name;
        out.add(path);
        for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
            enumerate(path, child.getKey(), child.getValue(), out);
        }
    }

    @Override
    public String canonicalToken(EntityModel<?> model, String storedToken) {
        if (!handles(model)) {
            return storedToken;
        }
        // A legacy #N token: resolve it to the concrete part (old index semantics), then report that part's
        // new path — this is the forward mapping the token migration writes into the data.
        int legacy = legacyIndex(storedToken);
        if (legacy >= 0) {
            ModelPart part = legacyField(model, legacy);
            String path = part == null ? null : pathOf(model, part);
            return path != null ? path : storedToken;
        }
        // First part (pre-order) whose path suffix-matches the stored token — the same order the pose walk
        // below follows, so canonical naming and attachment agree.
        for (String path : enumerateParts(model)) {
            if (EyeAttachmentResolver.pathMatches(storedToken, path)) {
                return path;
            }
        }
        return storedToken; // no part matches; leave the token as authored
    }

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        if (!handles(model)) {
            return false;
        }
        // Legacy #N: reproduce the old ReflectionResolver exactly — a single translateAndRotate on the
        // indexed top-level field, no ancestor walk — so pre-migration configs render unchanged.
        int legacy = legacyIndex(partToken);
        if (legacy >= 0) {
            ModelPart part = legacyField(model, legacy);
            if (part == null) {
                return false;
            }
            part.translateAndRotate(poseStack);
            return true;
        }

        Matrix4f[] capPose = new Matrix4f[1];
        Matrix3f[] capNormal = new Matrix3f[1];
        for (NamedRoot root : roots(model)) {
            walk(poseStack, "", root.name(), root.part(), partToken, capPose, capNormal);
            if (capPose[0] != null) {
                break;
            }
        }
        if (capPose[0] == null) {
            return false;
        }
        poseStack.last().pose().set(capPose[0]);
        poseStack.last().normal().set(capNormal[0]);
        return true;
    }

    private static void walk(PoseStack pose, String prefix, String name, ModelPart part, String token,
                             Matrix4f[] capPose, Matrix3f[] capNormal) {
        if (capPose[0] != null) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        String path = prefix.isEmpty() ? name : prefix + "/" + name;
        if (EyeAttachmentResolver.pathMatches(token, path)) {
            // Copy: the pose entry is reused/popped after this returns.
            capPose[0] = new Matrix4f(pose.last().pose());
            capNormal[0] = new Matrix3f(pose.last().normal());
        } else {
            for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
                walk(pose, path, child.getKey(), child.getValue(), token, capPose, capNormal);
            }
        }
        pose.popPose();
    }

    /** The full path (e.g. {@code root/body/head}) of {@code target} within this model, or null if absent. */
    @Nullable
    protected String pathOf(EntityModel<?> model, ModelPart target) {
        for (NamedRoot root : roots(model)) {
            String path = pathOf("", root.name(), root.part(), target);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    @Nullable
    private static String pathOf(String prefix, String name, ModelPart part, ModelPart target) {
        String path = prefix.isEmpty() ? name : prefix + "/" + name;
        if (part == target) {
            return path;
        }
        for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
            String found = pathOf(path, child.getKey(), child.getValue(), target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // --- legacy #N support ---

    /** The numeric index of a {@code #N} token, or -1 if the token isn't a bare {@code #<digits>}. */
    private static int legacyIndex(String token) {
        if (token == null || token.length() < 2 || token.charAt(0) != '#') {
            return -1;
        }
        for (int i = 1; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return -1;
            }
        }
        return Integer.parseInt(token.substring(1));
    }

    @Nullable
    private static ModelPart legacyField(EntityModel<?> model, int index) {
        List<ModelPart> fields = LEGACY_FIELDS.computeIfAbsent(model, ModelPartTreeResolver::reflectAllFields);
        return index >= 0 && index < fields.size() ? fields.get(index) : null;
    }

    /** Every {@link ModelPart} field, superclass-first, unfiltered (the old {@code #N} index order). */
    private static List<ModelPart> reflectAllFields(EntityModel<?> model) {
        Set<ModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ModelPart> ordered = new ArrayList<>();
        collectFields(model, model.getClass(), seen, ordered);
        return ordered;
    }

    private static void collectFields(EntityModel<?> model, Class<?> cls, Set<ModelPart> seen, List<ModelPart> out) {
        if (cls == null || cls == Object.class) {
            return;
        }
        collectFields(model, cls.getSuperclass(), seen, out); // superclass-first
        for (Field field : cls.getDeclaredFields()) {
            if (field.getType() != ModelPart.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                if (field.get(model) instanceof ModelPart part && seen.add(part)) {
                    out.add(part);
                }
            } catch (Throwable accessDenied) {
                // Module/access restriction or anything else: skip this field.
            }
        }
    }
}
