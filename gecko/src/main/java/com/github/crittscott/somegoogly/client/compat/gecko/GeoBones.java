package com.github.crittscott.somegoogly.client.compat.gecko;

import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.ModelMemo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * GeckoLib bone access — the GeckoLib analog of vanilla part resolution. <b>Only loaded when GeckoLib
 * is present</b> (reached through {@code GeckoCompatImpl}); it references GeckoLib types directly.
 *
 * <p>Bones are named in the {@code .geo.json}, so enumeration yields real names (better than the
 * vanilla reflection family's {@code #N}). There is deliberately no "move the pose to a bone" here:
 * a GeckoLib layer can only draw at a bone from inside {@code GeoRenderLayer#renderForBone}, where
 * GeckoLib hands it the fully-composed pose — re-walking bone transforms outside the model render
 * misses the entity's body rotations (see {@link GooglyGeoLayer}). So lookup returns the {@link GeoBone}
 * itself, matched by identity at render time.
 *
 * <p>NOTE: this binds directly to the GeckoLib 4.7.4 API ({@code BakedGeoModel#topLevelBones},
 * {@code GeoBone#getName}/{@code getChildBones}). Those names can change between GeckoLib versions,
 * so this is the first place to look if a GeckoLib update breaks bone attachment.
 */
public final class GeoBones {

    /**
     * Bones resolved once per (baked model, token) and matched by identity thereafter. Keying on the baked
     * model — not the entity — is what makes a model swap correct: a swapped-in model is a different
     * {@link BakedGeoModel} with its own entries, including its own cached "this token names no bone".
     *
     * <p>Needs no explicit clearing, unlike the vanilla-side caches: a {@link GeoBone} holds its parent and
     * children but never the {@link BakedGeoModel}, so nothing here keeps its own weak key alive, and a
     * baked model discarded on resource reload takes its entries with it.
     */
    private static final ModelMemo<BakedGeoModel, GeoBone> BONES = new ModelMemo<>();

    private GeoBones() {
    }

    private static void collect(GeoBone bone, List<String> out) {
        if (bone.getName() != null && !bone.getName().isEmpty()) {
            out.add(bone.getName());
        }
        for (GeoBone child : bone.getChildBones()) {
            collect(child, out);
        }
    }

    public static List<String> enumerate(BakedGeoModel model) {
        List<String> names = new ArrayList<>();
        for (GeoBone bone : model.topLevelBones()) {
            collect(bone, names);
        }
        return names;
    }

    /**
     * First bone (depth-first, so a stable order) whose root→bone name path suffix-matches
     * {@code token} per {@link EyeAttachmentResolver#pathMatches}, or {@code null}. A bare bone name
     * matches like {@code BakedGeoModel#getBone}; a slash path disambiguates same-named bones under
     * different parents, keeping GeckoLib tokens in the same vocabulary as the vanilla resolvers.
     */
    @Nullable
    public static GeoBone findBone(BakedGeoModel model, String token) {
        return BONES.get(model, token, GeoBones::resolveBone);
    }

    @Nullable
    private static GeoBone resolveBone(BakedGeoModel model, String token) {
        for (GeoBone bone : model.topLevelBones()) {
            GeoBone found = search(bone, "", token);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Nullable
    private static GeoBone search(GeoBone bone, String parentPath, String token) {
        String name = bone.getName() == null ? "" : bone.getName();
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        if (EyeAttachmentResolver.pathMatches(token, path)) {
            return bone;
        }
        for (GeoBone child : bone.getChildBones()) {
            GeoBone found = search(child, path, token);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
