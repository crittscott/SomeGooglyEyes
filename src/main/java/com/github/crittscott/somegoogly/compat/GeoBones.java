package com.github.crittscott.somegoogly.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * GeckoLib bone access — the GeckoLib analog of vanilla part resolution. <b>Only loaded when GeckoLib
 * is present</b> (reached through {@link GeckoCompat}); it references GeckoLib types directly.
 *
 * <p>Bones are named in the {@code .geo.json}, so enumeration yields real names (better than the
 * vanilla reflection family's {@code #N}). Positioning walks root→bone applying each bone's local
 * transform via {@code RenderUtils.prepMatrixForBone} — the same accumulation GeckoLib does while
 * rendering, so it reproduces the bone's animated pose.
 *
 * <p>NOTE: the exact GeckoLib 4.7.4 API (method names on {@code BakedGeoModel}/{@code GeoBone} and
 * {@code RenderUtils.prepMatrixForBone}) is the main thing to confirm on build.
 */
public final class GeoBones {

    private GeoBones() {
    }

    public static List<String> enumerate(BakedGeoModel model) {
        List<String> names = new ArrayList<>();
        for (GeoBone bone : model.topLevelBones()) {
            collect(bone, names);
        }
        return names;
    }

    private static void collect(GeoBone bone, List<String> out) {
        if (bone.getName() != null && !bone.getName().isEmpty()) {
            out.add(bone.getName());
        }
        for (GeoBone child : bone.getChildBones()) {
            collect(child, out);
        }
    }

    /** Move {@code poseStack} from the model base into the named bone's animated space. */
    public static boolean moveTo(PoseStack poseStack, BakedGeoModel model, String boneName) {
        Optional<GeoBone> target = model.getBone(boneName);
        if (target.isEmpty()) {
            return false;
        }
        Deque<GeoBone> chain = new ArrayDeque<>();
        for (GeoBone b = target.get(); b != null; b = b.getParent()) {
            chain.addFirst(b);
        }
        for (GeoBone b : chain) {
            RenderUtils.prepMatrixForBone(poseStack, b);
        }
        return true;
    }
}
