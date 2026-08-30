package com.github.crittscott.somegoogly.client.compat.neoforge;

import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.ModelMemo;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Bone enumeration and path resolution for the optional GeckoLib integration. */
public final class GeoBones {

    private static final ModelMemo<BakedGeoModel, GeoBone> BONES = new ModelMemo<>();

    private GeoBones() {
    }

    public static List<String> enumerate(BakedGeoModel model) {
        List<String> names = new ArrayList<>();
        for (GeoBone bone : model.topLevelBones()) {
            collect(bone, names);
        }
        return names;
    }

    private static void collect(GeoBone bone, List<String> names) {
        if (bone.getName() != null && !bone.getName().isEmpty()) {
            names.add(bone.getName());
        }
        for (GeoBone child : bone.getChildBones()) {
            collect(child, names);
        }
    }

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
