package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;

/**
 * GeckoLib bone access — the GeckoLib analog of vanilla part resolution. <b>Only loaded when GeckoLib
 * is present</b> (reached through {@link GeckoCompat}); it references GeckoLib types directly.
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
    public static GeoBone findBone(BakedGeoModel model, String token) {
        for (GeoBone bone : model.topLevelBones()) {
            GeoBone found = findBone(bone, "", token);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static GeoBone findBone(GeoBone bone, String parentPath, String token) {
        String name = bone.getName() == null ? "" : bone.getName();
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        if (EyeAttachmentResolver.pathMatches(token, path)) {
            return bone;
        }
        for (GeoBone child : bone.getChildBones()) {
            GeoBone found = findBone(child, path, token);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
