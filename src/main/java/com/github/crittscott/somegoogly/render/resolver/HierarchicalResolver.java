package com.github.crittscott.somegoogly.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Resolver for the {@link HierarchicalModel} family (retains a named root + child maps).
 *
 * <p>Uses the public {@link net.minecraft.client.model.geom.ModelPart#visit} walk: it applies every
 * ancestor's {@code translateAndRotate} and yields each cube's fully-accumulated pose plus a path
 * string built from the modder's string child-map keys (e.g. {@code /root/body/head}). We capture the
 * pose of the first cube whose final path segment matches the requested token (normalised so a
 * camelCase config token like {@code leftHead} matches a {@code left_head} key).
 *
 * <p>This is the mechanism validated in the Phase 0 spike: production-safe (string names survive
 * obfuscation) and it inherits all head/body/keyframe animation automatically.
 */
public class HierarchicalResolver implements EyeAttachmentResolver {

    @Override
    public boolean handles(EntityModel<?> model) {
        return model instanceof HierarchicalModel;
    }

    @Override
    public boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        String wanted = EyeAttachmentResolver.normalize(partToken);
        Matrix4f[] capPose = new Matrix4f[1];
        Matrix3f[] capNormal = new Matrix3f[1];

        // visit() pushes/pops on the PoseStack internally, leaving it unchanged on return.
        ((HierarchicalModel<?>) model).root().visit(poseStack, (pose, path, index, cube) -> {
            if (capPose[0] == null && wanted.equals(EyeAttachmentResolver.normalize(lastSegment(path)))) {
                // Copy: the Pose reference is reused/popped by the stack after visit returns.
                capPose[0] = new Matrix4f(pose.pose());
                capNormal[0] = new Matrix3f(pose.normal());
            }
        });

        if (capPose[0] == null) {
            return false;
        }
        poseStack.last().pose().set(capPose[0]);
        poseStack.last().normal().set(capNormal[0]);
        return true;
    }

    @Override
    public List<String> enumerateParts(EntityModel<?> model) {
        if (!(model instanceof HierarchicalModel)) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        ((HierarchicalModel<?>) model).root().visit(new PoseStack(), (pose, path, index, cube) -> {
            String segment = lastSegment(path);
            if (!segment.isEmpty()) {
                names.add(segment);
            }
        });
        return new ArrayList<>(names);
    }

    private static String lastSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
